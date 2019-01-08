package org.embulk.input.jira.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;
import org.embulk.config.ConfigException;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.spi.Exec;
import org.embulk.spi.util.RetryExecutor.RetryGiveupException;
import org.embulk.spi.util.RetryExecutor.Retryable;
import org.slf4j.Logger;

import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Base64.getEncoder;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.embulk.spi.util.RetryExecutor.retryExecutor;

public class JiraUtil
{
    private JiraUtil() {}

    private static final int CONNECTION_TIME_OUT = 300000;

    private static final Logger LOGGER = Exec.getLogger(JiraUtil.class);

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

    public static void checkUserCredentials(final PluginTask task)
    {
        try {
            authorizeAndRequestWithGet(task, buildPermissionUrl(task.getUri()));
        }
        catch (JiraException e) {
            LOGGER.error(String.format("JIRA return status (%s), reason (%s)", e.getStatusCode(), e.getMessage()));
            if (e.getStatusCode() == HttpStatus.SC_UNAUTHORIZED) {
                throw new ConfigException("Could not authorize with your credential.");
            }
            else {
                throw new ConfigException("Could not authorize with your credential due to problem when contacting JIRA API");
            }
        }
    }

    public static JsonObject searchIssues(final PluginTask task, int startAt, int maxResults)
    {
        String response = searchJiraAPI(task, startAt, maxResults);
        JsonParser parser = new JsonParser();
        JsonObject result = parser.parse(response).getAsJsonObject();
        return result;
    }

    public static int getTotalCount(final PluginTask task)
    {
        String response = searchJiraAPI(task, 0, 1);
        JsonParser parser = new JsonParser();
        JsonObject result = parser.parse(response).getAsJsonObject();
        return result.get("total").getAsInt();
    }

    private static String searchJiraAPI(final PluginTask task, int startAt, int maxResults)
    {
        try {
            return retryExecutor().withRetryLimit(task.getRetryLimit())
            .withInitialRetryWait(task.getInitialRetryIntervalMillis())
            .withMaxRetryWait(task.getMaximumRetryIntervalMillis())
            .runInterruptible(new Retryable<String>()
            {
                @Override
                public String call() throws Exception
                {
                    return authorizeAndRequestWithGet(task, buildSearchUrl(task, startAt, maxResults));
                }

                @Override
                public boolean isRetryableException(Exception exception)
                {
                    if (exception instanceof JiraException) {
                        int statusCode = ((JiraException) exception).getStatusCode();
                        if (statusCode / 100 == 4 && statusCode != HttpStatus.SC_UNAUTHORIZED && statusCode != 429) {
                            return false;
                        }
                        return true;
                    }
                    return false;
                }

                @Override
                public void onRetry(Exception exception, int retryCount, int retryLimit, int retryWait)
                        throws RetryGiveupException
                {
                    if (exception instanceof JiraException) {
                        String message = String
                                .format("Retrying %d/%d after %d seconds. HTTP status code: %s",
                                        retryCount, retryLimit,
                                        retryWait / 1000,
                                        ((JiraException) exception).getStatusCode());
                        LOGGER.warn(message);
                    }
                    else {
                        String message = String
                                .format("Retrying %d/%d after %d seconds. Message: %s",
                                        retryCount, retryLimit,
                                        retryWait / 1000,
                                        exception.getMessage());
                        LOGGER.warn(message, exception);
                    }
                }

                @Override
                public void onGiveup(Exception firstException, Exception lastException) throws RetryGiveupException
                {
                    LOGGER.warn("Retry Limits Completed");
                }
            });
        }
        catch (RetryGiveupException | InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private static String authorizeAndRequestWithGet(final PluginTask task, String url) throws JiraException
    {
        try {
            HttpClient client = createHttpClient();
            HttpGet request = createGetRequest(client, task, url);
            HttpResponse response = client.execute(request);
            // Check for HTTP response code : 200 : SUCCESS
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK) {
                throw new JiraException(statusCode,
                        "JIRA API Request Failed: " + EntityUtils.toString(response.getEntity()));
            }
            return EntityUtils.toString(response.getEntity());
        }
        catch (IOException e) {
            throw new JiraException(-1, e.getMessage());
        }
    }

    private static String buildPermissionUrl(String url)
    {
        UriBuilder builder = UriBuilder.fromUri(url);
        URI uri = builder.path("rest")
                        .path("api")
                        .path("latest")
                        .path("myself").build();
        return uri.toString();
    }

    private static String buildSearchUrl(PluginTask task, int startAt, int maxResults)
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
        LOGGER.info(uri.toString());
        return uri.toString();
    }

    private static HttpClient createHttpClient()
    {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIME_OUT)
                .setConnectionRequestTimeout(CONNECTION_TIME_OUT)
                .build();
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        return client;
    }

    private static HttpGet createGetRequest(HttpClient client, PluginTask task, String url)
    {
        HttpGet request = new HttpGet(url);
        switch (task.getAuthMethod()) {
        default:
            request.setHeader(
                    AUTHORIZATION,
                    String.format("Basic %s",
                                getEncoder().encodeToString(String.format("%s:%s",
                                task.getUsername(),
                                task.getPassword()).getBytes())));
            request.setHeader(ACCEPT, "application/json");
            request.setHeader(CONTENT_TYPE, "application/json");
            break;
        }
        return request;
    }

    public static int calculateTotalPage(int totalCount, int resultPerPage)
    {
        return (int) Math.ceil((double) totalCount / resultPerPage);
    }
}
