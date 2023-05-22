package org.embulk.input.jira.util;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.input.jira.Issue;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.spi.Column;
import org.embulk.spi.ColumnVisitor;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.Schema;
import org.embulk.util.config.units.ColumnConfig;
import org.embulk.util.json.JsonParser;
import org.embulk.util.timestamp.TimestampFormatter;

import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Strings.isNullOrEmpty;
import static org.embulk.input.jira.Constant.CREDENTIAL_URI_PATH;
import static org.embulk.input.jira.Constant.DEFAULT_TIMESTAMP_PATTERN;
import static org.embulk.input.jira.Constant.HTTP_TIMEOUT;
import static org.embulk.input.jira.Constant.SEARCH_URI_PATH;

public final class JiraUtil
{
    private JiraUtil() {}

    public static int calculateTotalPage(final int totalCount, final int resultPerPage)
    {
        return (int) Math.ceil((double) totalCount / resultPerPage);
    }

    public static String buildPermissionUrl(final String url)
    {
        return UriBuilder.fromUri(url).path(CREDENTIAL_URI_PATH).build().toString();
    }

    public static String buildSearchUrl(final String url)
    {
        return UriBuilder.fromUri(url).path(SEARCH_URI_PATH).build().toString();
    }

    public static void validateTaskConfig(final PluginTask task)
    {
        final String username = task.getUsername();
        if (isNullOrEmpty(username)) {
            throw new ConfigException("Username or email could not be empty");
        }
        final String password = task.getPassword();
        if (isNullOrEmpty(password)) {
            throw new ConfigException("Password could not be empty");
        }
        final String uri = task.getUri();
        if (isNullOrEmpty(uri)) {
            throw new ConfigException("JIRA API endpoint could not be empty");
        }
        try (CloseableHttpClient client = HttpClientBuilder.create()
                                            .setDefaultRequestConfig(RequestConfig.custom()
                                                                                .setConnectTimeout(HTTP_TIMEOUT)
                                                                                .setConnectionRequestTimeout(HTTP_TIMEOUT)
                                                                                .setSocketTimeout(HTTP_TIMEOUT)
                                                                                .setCookieSpec(CookieSpecs.STANDARD)
                                                                                .build())
                                            .build()) {
            final HttpGet request = new HttpGet(uri);
            try (CloseableHttpResponse response = client.execute(request)) {
                response.getStatusLine().getStatusCode();
            }
        }
        catch (IOException | IllegalArgumentException e) {
            throw new ConfigException("JIRA API endpoint is incorrect or not available");
        }
        final int retryInitialWaitSec = task.getInitialRetryIntervalMillis();
        if (retryInitialWaitSec < 1) {
            throw new ConfigException("Initial retry delay should be equal or greater than 1");
        }
        final int retryLimit = task.getRetryLimit();
        if (retryLimit < 0 || retryLimit > 10) {
            throw new ConfigException("Retry limit should between 0 and 10");
        }
    }

    /*
     * For getting the timestamp value of the node
     * Sometime if the parser could not parse the value then return null
     * */
    private static Instant getTimestampValue(final PluginTask task, final Column column, final String value)
    {
        final List<ColumnConfig> columnConfigs = task.getColumns().getColumns();
        String pattern = DEFAULT_TIMESTAMP_PATTERN;
        for (final ColumnConfig columnConfig : columnConfigs) {
            final ConfigSource columnConfigSource = columnConfig.getConfigSource();
            if (columnConfig.getName().equals(column.getName())
                    && columnConfigSource != null
                    && columnConfigSource.has("format")) {
                pattern = columnConfigSource.get(String.class, "format");
                break;
            }
        }
        final TimestampFormatter formatter = TimestampFormatter
                .builder(pattern, true)
                .setDefaultZoneFromString("UTC")
                .build();
        try {
            return formatter.parse(value);
        }
        catch (final Exception e) {
            return null;
        }
    }

    /*
     * For getting the Long value of the node
     * Sometime if error occurs (i.e a JSON value but user modified it as long) then return null
     * */
    private static Long getLongValue(final JsonElement value)
    {
        try {
            return value.getAsLong();
        }
        catch (final Exception e) {
            return null;
        }
    }

    /*
     * For getting the Double value of the node
     * Sometime if error occurs (i.e a JSON value but user modified it as double) then return null
     * */
    private static Double getDoubleValue(final JsonElement value)
    {
        try {
            return value.getAsDouble();
        }
        catch (final Exception e) {
            return null;
        }
    }

    /*
     * For getting the Boolean value of the node
     * Sometime if error occurs (i.e a JSON value but user modified it as boolean) then return null
     * */
    private static Boolean getBooleanValue(final JsonElement value)
    {
        try {
            return value.getAsBoolean();
        }
        catch (final Exception e) {
            return null;
        }
    }

    public static void addRecord(final Issue issue, final Schema schema, final PluginTask task, final PageBuilder pageBuilder)
    {
        schema.visitColumns(new ColumnVisitor() {
            @Override
            public void jsonColumn(final Column column)
            {
                final JsonElement data = issue.getValue(column.getName());
                if (data.isJsonNull() || data.isJsonPrimitive()) {
                    pageBuilder.setNull(column);
                }
                else {
                    pageBuilder.setJson(column, new JsonParser().parse(data.toString()));
                }
            }

            @Override
            public void stringColumn(final Column column)
            {
                final JsonElement data = issue.getValue(column.getName());
                if (data.isJsonNull()) {
                    pageBuilder.setNull(column);
                }
                else if (data.isJsonPrimitive()) {
                    pageBuilder.setString(column, data.getAsString());
                }
                else if (data.isJsonArray()) {
                    pageBuilder.setString(column, StreamSupport.stream(data.getAsJsonArray().spliterator(), false)
                            .map(obj -> {
                                if (obj.isJsonPrimitive()) {
                                    return obj.getAsString();
                                }
                                return obj.toString();
                            })
                            .collect(Collectors.joining(",")));
                }
                else {
                    pageBuilder.setString(column, data.toString());
                }
            }

            @Override
            public void timestampColumn(final Column column)
            {
                final JsonElement data = issue.getValue(column.getName());
                if (data.isJsonNull() || data.isJsonObject() || data.isJsonArray()) {
                    pageBuilder.setNull(column);
                }
                else {
                    final Instant value = getTimestampValue(task, column, data.getAsString());
                    if (value == null) {
                        pageBuilder.setNull(column);
                    }
                    else {
                        pageBuilder.setTimestamp(column, value);
                    }
                }
            }

            @Override
            public void booleanColumn(final Column column)
            {
                final Boolean value = getBooleanValue(issue.getValue(column.getName()));
                if (value == null) {
                    pageBuilder.setNull(column);
                }
                else {
                    pageBuilder.setBoolean(column, value);
                }
            }

            @Override
            public void longColumn(final Column column)
            {
                final Long value = getLongValue(issue.getValue(column.getName()));
                if (value == null) {
                    pageBuilder.setNull(column);
                }
                else {
                    pageBuilder.setLong(column, value);
                }
            }

            @Override
            public void doubleColumn(final Column column)
            {
                final Double value = getDoubleValue(issue.getValue(column.getName()));
                if (value == null) {
                    pageBuilder.setNull(column);
                }
                else {
                    pageBuilder.setDouble(column, value);
                }
            }
        });
        pageBuilder.addRecord();
    }

    public static LinkedHashMap<String, Object> toLinkedHashMap(final JsonObject flt)
    {
        final LinkedHashMap<String, Object> result = new LinkedHashMap<>();
        for (final String key : flt.keySet()) {
            final JsonElement elem = flt.get(key);
            if (elem.isJsonPrimitive()) {
                result.put(key, flt.get(key).getAsString());
            }
            else {
                result.put(key, elem);
            }
        }
        return result;
    }
}
