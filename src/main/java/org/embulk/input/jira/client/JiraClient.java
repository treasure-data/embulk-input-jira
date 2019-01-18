package org.embulk.input.jira.client;

import com.google.gson.JsonElement;
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
import org.embulk.input.jira.Issue;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.input.jira.util.JiraException;
import org.embulk.input.jira.util.JiraUtil;
import org.embulk.spi.Exec;
import org.embulk.spi.util.RetryExecutor.RetryGiveupException;
import org.embulk.spi.util.RetryExecutor.Retryable;
import org.slf4j.Logger;

import java.io.IOException;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static java.util.Base64.getEncoder;
import static org.apache.http.HttpHeaders.ACCEPT;
import static org.apache.http.HttpHeaders.AUTHORIZATION;
import static org.apache.http.HttpHeaders.CONTENT_TYPE;
import static org.embulk.input.jira.Constant.MIN_RESULTS;
import static org.embulk.spi.util.RetryExecutor.retryExecutor;

public class JiraClient
{
    public JiraClient() {}

    private static final int CONNECTION_TIME_OUT = 300000;

    private static final Logger LOGGER = Exec.getLogger(JiraClient.class);

    public void checkUserCredentials(final PluginTask task)
    {
        try {
            authorizeAndRequestWithGet(task, JiraUtil.buildPermissionUrl(task.getUri()));
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

    public List<Issue> searchIssues(final PluginTask task, int startAt, int maxResults)
    {
        String response = searchJiraAPI(task, startAt, maxResults);
        JsonParser parser = new JsonParser();
        JsonObject result = parser.parse(response).getAsJsonObject();
        return StreamSupport.stream(result.get("issues").getAsJsonArray().spliterator(), false)
                            .map(jsonElement -> {
                                JsonObject json = jsonElement.getAsJsonObject();
                                JsonObject fields = json.get("fields").getAsJsonObject();
                                Set<Entry<String, JsonElement>> entries = fields.entrySet();
                                json.remove("fields");
                                // Merged all properties in fields to the object
                                for (Entry<String, JsonElement> entry : entries) {
                                    json.add(entry.getKey(), entry.getValue());
                                }
                                return new Issue(json);
                            })
                            .collect(Collectors.toList());
    }

    public int getTotalCount(final PluginTask task)
    {
        String response = searchJiraAPI(task, 0, MIN_RESULTS);
        JsonParser parser = new JsonParser();
        JsonObject result = parser.parse(response).getAsJsonObject();
        return result.get("total").getAsInt();
    }

    private String searchJiraAPI(final PluginTask task, int startAt, int maxResults)
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
                    return authorizeAndRequestWithGet(task, JiraUtil.buildSearchUrl(task, startAt, maxResults));
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

    private String authorizeAndRequestWithGet(final PluginTask task, String url) throws JiraException
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

    private HttpClient createHttpClient()
    {
        RequestConfig config = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIME_OUT)
                .setConnectionRequestTimeout(CONNECTION_TIME_OUT)
                .build();
        HttpClient client = HttpClientBuilder.create().setDefaultRequestConfig(config).build();
        return client;
    }

    private HttpGet createGetRequest(HttpClient client, PluginTask task, String url)
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
}
