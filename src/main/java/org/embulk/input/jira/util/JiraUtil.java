package org.embulk.input.jira.util;

import com.google.gson.JsonElement;

import org.embulk.config.ConfigException;
import org.embulk.input.jira.Issue;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnConfig;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Schema;
import org.embulk.spi.json.JsonParser;
import org.embulk.spi.time.Timestamp;
import org.embulk.spi.time.TimestampParser;

import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static com.google.common.base.Strings.isNullOrEmpty;

public final class JiraUtil
{
    private JiraUtil() {}

    public static int calculateTotalPage(int totalCount, int resultPerPage)
    {
        return (int) Math.ceil((double) totalCount / resultPerPage);
    }

    public static String buildPermissionUrl(String url)
    {
        UriBuilder builder = UriBuilder.fromUri(url);
        URI uri = builder.path("rest")
                        .path("api")
                        .path("latest")
                        .path("myself").build();
        return uri.toString();
    }

    public static String buildSearchUrl(PluginTask task, int startAt, int maxResults)
    {
        UriBuilder builder = UriBuilder.fromUri(task.getUri());
        URI uri = builder.path("rest")
                        .path("api")
                        .path("latest")
                        .path("search")
                        .queryParam("jql", task.getJQL())
                        .queryParam("startAt", startAt)
                        .queryParam("maxResults", maxResults)
                        .queryParam("fields", "*all")
                        .build();
        return uri.toString();
    }

    public static void validateTaskConfig(final PluginTask task)
    {
        String username = task.getUsername();
        if (isNullOrEmpty(username)) {
            throw new ConfigException("Username or email could not be empty");
        }
        String password = task.getPassword();
        if (isNullOrEmpty(password)) {
            throw new ConfigException("Password could not be empty");
        }
        String uri = task.getUri();
        if (isNullOrEmpty(uri)) {
            throw new ConfigException("JIRA API endpoint could not be empty");
        }
        HttpURLConnection connection = null;
        try {
            URL u = new URL(uri);
            connection = (HttpURLConnection) u.openConnection();
            connection.setRequestMethod("GET");
            connection.getResponseCode();
        }
        catch (IOException e) {
            throw new ConfigException("JIRA API endpoint is incorrect or not available");
        }
        finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        String jql = task.getJQL();
        if (isNullOrEmpty(jql)) {
            throw new ConfigException("JQL could not be empty");
        }
        int retryInitialWaitSec = task.getInitialRetryIntervalMillis();
        if (retryInitialWaitSec < 1) {
            throw new ConfigException("Initial retry delay should be equal or greater than 1");
        }
        int retryLimit = task.getRetryLimit();
        if (retryLimit < 0 || retryLimit > 10) {
            throw new ConfigException("Retry limit should between 0 and 10");
        }
    }

    private static Timestamp getTimestampValue(PluginTask task, Column column, String value)
    {
        List<ColumnConfig> columnConfigs = task.getColumns().getColumns();
        String pattern = "%Y-%m-%dT%H:%M:%S.%L%z";
        for (ColumnConfig config : columnConfigs) {
            if (config.getName().equals(column.getName()) && config.getConfigSource() != null && config.getConfigSource().getObjectNode() != null && config.getConfigSource().getObjectNode().get("format").isTextual()) {
                pattern = config.getConfigSource().getObjectNode().get("format").asText();
                break;
            }
        }
        TimestampParser parser = TimestampParser.of(pattern, "UTC");
        return parser.parse(value);
    }

    public static void addRecord(Issue issue, Schema schema, PluginTask task, PageBuilder pageBuilder)
    {
        schema.visitColumns(new ColumnVisitor() {
            @Override
            public void timestampColumn(Column column)
            {
                JsonElement value = issue.fetchValue(column.getName());
                if (value.isJsonNull()) {
                    pageBuilder.setNull(column);
                }
                else {
                    pageBuilder.setTimestamp(column, getTimestampValue(task, column, value.getAsString()));
                }
            }

            @Override
            public void stringColumn(Column column)
            {
                JsonElement value = issue.fetchValue(column.getName());
                if (value.isJsonNull()) {
                    pageBuilder.setNull(column);
                }
                else if (value.isJsonPrimitive()) {
                    pageBuilder.setString(column, value.getAsString());
                }
                else {
                    pageBuilder.setString(column, value.toString());
                }
            }

            @Override
            public void longColumn(Column column)
            {
                JsonElement value = issue.fetchValue(column.getName());
                if (value.isJsonPrimitive()) {
                    pageBuilder.setLong(column, value.getAsLong());
                }
                else {
                    pageBuilder.setNull(column);
                }
            }

            @Override
            public void jsonColumn(Column column)
            {
                JsonElement value = issue.fetchValue(column.getName());
                if (value.isJsonNull()) {
                    pageBuilder.setNull(column);
                }
                else {
                    pageBuilder.setJson(column, new JsonParser().parse(value.toString()));
                }
            }

            @Override
            public void doubleColumn(Column column)
            {
                JsonElement value = issue.fetchValue(column.getName());
                if (value.isJsonPrimitive()) {
                    pageBuilder.setDouble(column, value.getAsDouble());
                }
                else {
                    pageBuilder.setNull(column);
                }
            }

            @Override
            public void booleanColumn(Column column)
            {
                JsonElement value = issue.fetchValue(column.getName());
                if (value.isJsonPrimitive()) {
                    pageBuilder.setBoolean(column, value.getAsBoolean());
                }
                else {
                    pageBuilder.setNull(column);
                }
            }
        });
        pageBuilder.addRecord();
    }
}
