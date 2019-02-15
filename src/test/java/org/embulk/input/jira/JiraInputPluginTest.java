package org.embulk.input.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.DataSourceImpl;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.GuessExecutor;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.input.jira.client.JiraClient;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Schema;
import org.embulk.spi.TestPageBuilderReader.MockPageOutput;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraInputPluginTest
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();

    private JiraInputPlugin plugin;
    private JiraClient jiraClient;
    private JsonObject data;
    private ConfigSource config;
    private HttpClient client = Mockito.mock(HttpClient.class);
    private HttpResponse response = Mockito.mock(HttpResponse.class);
    private StatusLine statusLine = Mockito.mock(StatusLine.class);

    private MockPageOutput output = new MockPageOutput();
    private PageBuilder pageBuilder;
    private GuessExecutor guessExecutor = Mockito.mock(GuessExecutor.class);
    @Before
    public void setUp() throws IOException
    {
        if (plugin == null) {
            plugin = Mockito.spy(new JiraInputPlugin());
            jiraClient = Mockito.spy(new JiraClient());
            data = TestHelpers.getJsonFromFile("jira_input_plugin.json");
            config = TestHelpers.config();
            config.loadConfig(PluginTask.class);
            pageBuilder = Mockito.mock(PageBuilder.class);
            //pageBuilder = new PageBuilder(Exec.getBufferAllocator(), config.loadConfig(PluginTask.class).getColumns().toSchema(), output);
        }
        when(plugin.getJiraClient()).thenReturn(jiraClient);
//        when(plugin.getGuessExecutor()).thenReturn(guessExecutor);
        when(jiraClient.createHttpClient()).thenReturn(client);
        when(client.execute(Mockito.any(HttpUriRequest.class))).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
        doReturn(pageBuilder).when(plugin).getPageBuilder(Mockito.any(), Mockito.any());
        doReturn(guessExecutor).when(plugin).getGuessExecutor();
    }

    @Test
    public void test_run_withEmptyResult() throws IOException
    {
        JsonObject authorizeResponse = data.get("authenticateSuccess").getAsJsonObject();
        JsonObject searchResponse = data.get("emptyResult").getAsJsonObject();

        when(statusLine.getStatusCode())
            .thenReturn(authorizeResponse.get("statusCode").getAsInt())
            .thenReturn(searchResponse.get("statusCode").getAsInt());
        when(response.getEntity())
            .thenReturn(new StringEntity(authorizeResponse.get("body").toString()))
            .thenReturn(new StringEntity(searchResponse.get("body").toString()));

        plugin.transaction(config, new Control());
     // Check credential 1 + getTotal 1 + loadData 0
        verify(jiraClient, times(2)).createHttpClient();
        verify(pageBuilder, times(0)).addRecord();
        verify(pageBuilder, times(1)).finish();
    }

    @Test
    public void test_run_with1RecordsResult() throws IOException
    {
        JsonObject authorizeResponse = data.get("authenticateSuccess").getAsJsonObject();
        JsonObject searchResponse = data.get("oneRecordResult").getAsJsonObject();

        when(statusLine.getStatusCode())
            .thenReturn(authorizeResponse.get("statusCode").getAsInt())
            .thenReturn(searchResponse.get("statusCode").getAsInt());
        when(response.getEntity())
            .thenReturn(new StringEntity(authorizeResponse.get("body").toString()))
            .thenReturn(new StringEntity(searchResponse.get("body").toString()));

        plugin.transaction(config, new Control());
        // Check credential 1 + getTotal 1 + loadData 1
        verify(jiraClient, times(3)).createHttpClient();
        verify(pageBuilder, times(1)).addRecord();
        verify(pageBuilder, times(1)).finish();
    }

    @Test
    public void test_run_with2PagesResult() throws IOException
    {
        JsonObject authorizeResponse = data.get("authenticateSuccess").getAsJsonObject();
        JsonObject searchResponse = data.get("2PagesResult").getAsJsonObject();

        when(statusLine.getStatusCode())
            .thenReturn(authorizeResponse.get("statusCode").getAsInt())
            .thenReturn(searchResponse.get("statusCode").getAsInt());
        when(response.getEntity())
            .thenReturn(new StringEntity(authorizeResponse.get("body").toString()))
            .thenReturn(new StringEntity(searchResponse.get("body").toString()));

        plugin.transaction(config, new Control());
        // Check credential 1 + getTotal 1 + loadData 2
        verify(jiraClient, times(4)).createHttpClient();
        verify(pageBuilder, times(2)).addRecord();
        verify(pageBuilder, times(1)).finish();
    }

    @Test
    public void test_preview_withEmptyResult() throws IOException
    {
        when(plugin.isPreview()).thenReturn(true);
        JsonObject authorizeResponse = data.get("authenticateSuccess").getAsJsonObject();
        JsonObject searchResponse = data.get("emptyResult").getAsJsonObject();

        when(statusLine.getStatusCode())
            .thenReturn(authorizeResponse.get("statusCode").getAsInt())
            .thenReturn(searchResponse.get("statusCode").getAsInt());
        when(response.getEntity())
            .thenReturn(new StringEntity(searchResponse.get("body").toString()));

        plugin.transaction(config, new Control());
        // Check credential 1 + loadData 1
        verify(jiraClient, times(2)).createHttpClient();
        verify(pageBuilder, times(0)).addRecord();
        verify(pageBuilder, times(1)).finish();
    }

    @Test
    public void test_preview_with1RecordsResult() throws IOException
    {
        when(plugin.isPreview()).thenReturn(true);
        JsonObject authorizeResponse = data.get("authenticateSuccess").getAsJsonObject();
        JsonObject searchResponse = data.get("oneRecordResult").getAsJsonObject();

        when(statusLine.getStatusCode())
            .thenReturn(authorizeResponse.get("statusCode").getAsInt())
            .thenReturn(searchResponse.get("statusCode").getAsInt());
        when(response.getEntity())
            .thenReturn(new StringEntity(authorizeResponse.get("body").toString()))
            .thenReturn(new StringEntity(searchResponse.get("body").toString()));

        plugin.transaction(config, new Control());
        // Check credential 1 + loadData 1
        verify(jiraClient, times(2)).createHttpClient();
        verify(pageBuilder, times(1)).addRecord();
        verify(pageBuilder, times(1)).finish();
    }

    @Test
    public void test_guess() throws IOException
    {
        ConfigSource configSource = TestHelpers.config();
        JsonObject authorizeResponse = data.get("authenticateSuccess").getAsJsonObject();
        JsonObject searchResponse = data.get("guessDataResult").getAsJsonObject();
        ObjectNode guessResult = (ObjectNode) new ObjectMapper().readTree(data.get("guessResult").toString());

        when(statusLine.getStatusCode())
            .thenReturn(authorizeResponse.get("statusCode").getAsInt())
            .thenReturn(searchResponse.get("statusCode").getAsInt());
        when(response.getEntity())
            .thenReturn(new StringEntity(searchResponse.get("body").toString()))
            .thenReturn(new StringEntity(searchResponse.get("body").toString()));

        when(guessExecutor.guessParserConfig(Mockito.any(), Mockito.any(), Mockito.any())).thenReturn(new DataSourceImpl(null, guessResult));

        ConfigDiff result = plugin.guess(configSource);
        JsonElement expected = data.get("guessResult");
        JsonElement actual = new JsonParser().parse(result.toString());
        assertEquals(expected, actual);
    }

    private class Control implements InputPlugin.Control
    {
        @Override
        public List<TaskReport> run(final TaskSource taskSource, final Schema schema, final int taskCount)
        {
            List<TaskReport> reports = new ArrayList<>();
            for (int i = 0; i < taskCount; i++) {
                reports.add(plugin.run(taskSource, schema, i, output));
            }
            return reports;
        }
    }
}
