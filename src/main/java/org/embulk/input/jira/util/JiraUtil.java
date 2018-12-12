package org.embulk.input.jira.util;

import com.atlassian.jira.rest.client.api.IssueRestClient;
import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.MyPermissionsRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.SearchRestClient;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.atlassian.jira.rest.client.api.domain.SearchResult;
import com.atlassian.jira.rest.client.internal.async.AsynchronousJiraRestClientFactory;
import com.google.common.base.Optional;

import io.atlassian.util.concurrent.Promise;

import org.embulk.config.ConfigException;
import org.embulk.input.jira.AuthenticateMethod;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Strings.isNullOrEmpty;
public class JiraUtil
{
    private JiraUtil() {}

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
        int retryInitialWaitSec = task.getRetryInitialWaitSec();
        if (retryInitialWaitSec < 1) {
            throw new ConfigException("Initial retry delay should be equal or greater than 1");
        }
        int retryLimit = task.getRetryLimit();
        if (retryLimit < 0 || retryLimit > 10) {
            throw new ConfigException("Retry limit should between 0 and 10");
        }
    }

    public static JiraRestClient createJiraRestClient(final PluginTask task) throws URISyntaxException
    {
        AuthenticateMethod authMethod = task.getAuthMethod();
        JiraRestClient client = null;
        // Currently only support basic authentication but will support more methods in the future
        switch (authMethod) {
        case BASIC:
            client = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(new URI(task.getUri()), task.getUsername(), task.getPassword());
            break;
        default:
            client = new AsynchronousJiraRestClientFactory().createWithBasicHttpAuthentication(new URI(task.getUri()), task.getUsername(), task.getPassword());
            break;
        }
        return client;
    }

    public static void checkUserCredentials(final JiraRestClient client, final PluginTask task)
    {
        MyPermissionsRestClient myPermissionsRestClient = client.getMyPermissionsRestClient();
        try {
            myPermissionsRestClient.getMyPermissions(null).claim();
        }
        catch (RestClientException e) {
            Optional<Integer> statusCode = e.getStatusCode();
            if (statusCode.isPresent() && statusCode.get().equals(401)) {
                throw new ConfigException("Could not authorize with your credential.");
            }
            throw new ConfigException(String.format("JIRA return \"%s\" Error ", statusCode.isPresent() ? statusCode.get() : "Unknown"));
        }
    }

    public static int getTotalCount(final JiraRestClient client, String jql)
    {
        SearchRestClient searchClient = client.getSearchClient();
        SearchResult result = searchClient.searchJql(jql, 1, 0, null).claim();
        return result.getTotal();
    }

    public static int calculateTotalPage(int totalCount, int resultPerPage)
    {
        return (int) Math.ceil((double) totalCount / resultPerPage);
    }

    public static List<String> getRawIssues(final JiraRestClient client, String jql, int startAt, int maxResults)
    {
        SearchRestClient searchClient = client.getSearchClient();
        Promise<SearchResult> result = searchClient.searchJql(jql, maxResults, startAt, null);
        return StreamSupport.stream(result.claim().getIssues().spliterator(), false).map(issue -> issue.getKey()).collect(Collectors.toList());
    }

    public static Issue getIssue(final JiraRestClient client, String issueKey)
    {
        IssueRestClient issueRestClient = client.getIssueClient();
        return issueRestClient.getIssue(issueKey).claim();
    }
}
