package org.embulk.input.jira.util;

import org.embulk.config.ConfigException;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;

import javax.ws.rs.core.UriBuilder;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URL;

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
}
