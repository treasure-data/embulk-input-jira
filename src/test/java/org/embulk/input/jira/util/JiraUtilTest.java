package org.embulk.input.jira.util;

import com.google.gson.JsonObject;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.input.jira.Issue;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.input.jira.TestHelpers;
import org.embulk.spi.Column;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Schema;
import org.embulk.util.json.JsonParser;
import org.embulk.util.timestamp.TimestampFormatter;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;
import org.msgpack.value.Value;

import java.io.IOException;
import java.time.Instant;

import static org.embulk.input.jira.JiraInputPlugin.CONFIG_MAPPER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class JiraUtilTest
{
    private static JsonObject data;
    private static PluginTask pluginTask;
    private static Schema schema;
    private static Column booleanColumn;
    private static Column longColumn;
    private static Column doubleColumn;
    private static Column stringColumn;
    private static Column dateColumn;
    private static Column jsonColumn;

    @BeforeClass
    public static void setUp() throws IOException
    {
        data = TestHelpers.getJsonFromFile("jira_util.json");
        pluginTask = CONFIG_MAPPER.map(TestHelpers.config(), PluginTask.class);
        schema = pluginTask.getColumns().toSchema();
        booleanColumn = schema.getColumn(0);
        longColumn = schema.getColumn(1);
        doubleColumn = schema.getColumn(2);
        stringColumn = schema.getColumn(3);
        dateColumn = schema.getColumn(4);
        jsonColumn = schema.getColumn(5);
    }

    @Test
    public void test_calculateTotalPage()
    {
        final int resultPerPage = 50;
        int expected = 0;
        int totalCount = 0;
        int actual = JiraUtil.calculateTotalPage(totalCount, resultPerPage);
        assertEquals(expected, actual);

        expected = 1;
        totalCount = resultPerPage - 1;
        actual = JiraUtil.calculateTotalPage(totalCount, resultPerPage);
        assertEquals(expected, actual);

        expected = 1;
        totalCount = resultPerPage;
        actual = JiraUtil.calculateTotalPage(totalCount, resultPerPage);
        assertEquals(expected, actual);

        expected = 2;
        totalCount = resultPerPage + 1;
        actual = JiraUtil.calculateTotalPage(totalCount, resultPerPage);
        assertEquals(expected, actual);
    }

    @Test
    public void test_buildPermissionUrl()
    {
        String url = "https://example.com";
        String expected = "https://example.com/rest/api/latest/myself";
        String actual = JiraUtil.buildPermissionUrl(url);
        assertEquals(expected, actual);

        url = "https://example.com/";
        expected = "https://example.com/rest/api/latest/myself";
        actual = JiraUtil.buildPermissionUrl(url);
        assertEquals(expected, actual);

        url = "https://example.com//";
        expected = "https://example.com//rest/api/latest/myself";
        actual = JiraUtil.buildPermissionUrl(url);
        assertEquals(expected, actual);

        url = "https://example.com/sub/subsub";
        expected = "https://example.com/sub/subsub/rest/api/latest/myself";
        actual = JiraUtil.buildPermissionUrl(url);
        assertEquals(expected, actual);
    }

    @Test
    public void test_buildSearchUrl() throws IOException
    {
        final PluginTask task = CONFIG_MAPPER.map(TestHelpers.config(), PluginTask.class);
        final String expected = "https://example.com/rest/api/latest/search/jql";
        final String actual = JiraUtil.buildSearchUrl(task.getUri());
        assertEquals(expected, actual);
    }

    @Test
    public void test_validateTaskConfig_allValid() throws IOException
    {
        final ConfigSource configSource = TestHelpers.config();
        final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
        JiraUtil.validateTaskConfig(task);
    }

    @Test
    public void test_validateTaskConfig_emptyUsername() throws IOException
    {
        final ConfigException exception = assertThrows("Username or email could not be empty", ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("username", "");
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("Username or email could not be empty", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_emptyPassword() throws IOException
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("password", "");
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("Password could not be empty", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_emptyUri() throws IOException
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("uri", "");
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("JIRA API endpoint could not be empty", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_nonExistedUri() throws IOException
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("uri", "https://not-existed-domain");
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("JIRA API endpoint is incorrect or not available", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_invalidUriProtocol()
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("uri", "ftp://example.com");
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("JIRA API endpoint is incorrect or not available", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_containSpaceUri()
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("uri", "https://example .com");
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("JIRA API endpoint is incorrect or not available", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_emptyJql()
    {
        final ConfigSource configSource = TestHelpers.config();
        configSource.set("jql", "");
        final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
        JiraUtil.validateTaskConfig(task);
    }

    @Test
    public void test_validateTaskConfig_missingJql()
    {
        final ConfigSource configSource = TestHelpers.config();
        configSource.remove("jql");
        final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
        JiraUtil.validateTaskConfig(task);
    }

    @Test
    public void test_validateTaskConfig_RetryIntervalIs0()
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("initial_retry_interval_millis", 0);
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("Initial retry delay should be equal or greater than 1", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_RetryIntervalIsNegative()
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("initial_retry_interval_millis", -1);
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("Initial retry delay should be equal or greater than 1", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_RetryLimitGreaterThan10()
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("retry_limit", 11);
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("Retry limit should between 0 and 10", exception.getMessage());
    }

    @Test
    public void test_validateTaskConfig_RetryLimitLessThan0()
    {
        final ConfigException exception = assertThrows(ConfigException.class, () -> {
            final ConfigSource configSource = TestHelpers.config();
            configSource.set("retry_limit", -1);
            final PluginTask task = CONFIG_MAPPER.map(configSource, PluginTask.class);
            JiraUtil.validateTaskConfig(task);
        });
        assertEquals("Retry limit should between 0 and 10", exception.getMessage());
    }

    @Test
    public void test_addRecord_allRight()
    {
        final String testName = "allRight";
        final Issue issue = new Issue(data.get(testName).getAsJsonObject());
        final PageBuilder mock = Mockito.mock(PageBuilder.class);

        final Boolean boolValue = Boolean.TRUE;
        final Long longValue = Long.valueOf(1);
        final Double doubleValue = Double.valueOf(1);
        final String stringValue = "string";
        final Instant dateValue = TimestampFormatter
                .builder("%Y-%m-%dT%H:%M:%S.%L%z", true)
                .setDefaultZoneFromString("UTC")
                .build().parse("2019-01-01T00:00:00.000Z");
        final Value jsonValue = new JsonParser().parse("{}");

        JiraUtil.addRecord(issue, schema, pluginTask, mock);

        verify(mock, times(1)).setBoolean(booleanColumn, boolValue);
        verify(mock, times(1)).setLong(longColumn, longValue);
        verify(mock, times(1)).setDouble(doubleColumn, doubleValue);
        verify(mock, times(1)).setString(stringColumn, stringValue);
        verify(mock, times(1)).setTimestamp(dateColumn, dateValue);
        verify(mock, times(1)).setJson(jsonColumn, jsonValue);
    }

    @Test
    public void test_addRecord_allWrong()
    {
        final String testName = "allWrong";
        final Issue issue = new Issue(data.get(testName).getAsJsonObject());
        final PageBuilder mock = Mockito.mock(PageBuilder.class);

        final String stringValue = "{}";
        final Value jsonValue = new JsonParser().parse("{}");

        JiraUtil.addRecord(issue, schema, pluginTask, mock);

        verify(mock, times(1)).setNull(booleanColumn);
        verify(mock, times(1)).setNull(longColumn);
        verify(mock, times(1)).setNull(doubleColumn);
        verify(mock, times(1)).setString(stringColumn, stringValue);
        verify(mock, times(1)).setNull(dateColumn);
        verify(mock, times(1)).setJson(jsonColumn, jsonValue);
    }

    @Test
    public void test_addRecord_allMissing()
    {
        final String testName = "allMissing";
        final Issue issue = new Issue(data.get(testName).getAsJsonObject());
        final PageBuilder mock = Mockito.mock(PageBuilder.class);

        JiraUtil.addRecord(issue, schema, pluginTask, mock);

        verify(mock, times(6)).setNull(Mockito.any(Column.class));
    }

    @Test
    public void test_addRecord_arrayAsString()
    {
        final String testName = "arrayAsString";
        final Issue issue = new Issue(data.get(testName).getAsJsonObject());
        final PageBuilder mock = Mockito.mock(PageBuilder.class);

        final String stringValue = "1,{},[]";

        JiraUtil.addRecord(issue, schema, pluginTask, mock);

        verify(mock, times(1)).setString(stringColumn, stringValue);
    }
}
