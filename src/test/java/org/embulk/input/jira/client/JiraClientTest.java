package org.embulk.input.jira.client;

import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.input.jira.Issue;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.input.jira.TestHelpers;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JiraClientTest
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();
    private JiraClient jiraClient;
    private PluginTask task;

    private HttpClient client = Mockito.mock(HttpClient.class);
    private HttpResponse response = Mockito.mock(HttpResponse.class);
    private StatusLine statusLine = Mockito.mock(StatusLine.class);
    private JsonObject data;

    @Before
    public void setUp() throws IOException
    {
        if (jiraClient == null) {
            jiraClient = Mockito.spy(new JiraClient());
            response = Mockito.mock(HttpResponse.class);
            task = TestHelpers.config().loadConfig(PluginTask.class);
            data = TestHelpers.getJsonFromFile("jira_client.json");
        }
        when(jiraClient.createHttpClient()).thenReturn(client);
        when(client.execute(Mockito.any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    public void test_checkUserCredentials_success() throws IOException
    {
        String dataName =  "credentialSuccess";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        jiraClient.checkUserCredentials(task);
    }

    @Test
    public void test_checkUserCredentials_failOn400() throws IOException
    {
        String dataName =  "credentialFail400";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_failOn401() throws IOException
    {
        String dataName =  "credentialFail401";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_failOn429() throws IOException
    {
        String dataName =  "credentialFail429";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential due to problems when contacting JIRA API.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_failOn500() throws IOException
    {
        String dataName =  "credentialFail500";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential due to problems when contacting JIRA API.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_getTotalCount_success() throws IOException
    {
        String dataName =  "totalCountSuccess";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        int totalCount = jiraClient.getTotalCount(task);
        assertEquals(totalCount, messageResponse.get("body").getAsJsonObject().get("total").getAsInt());
    }

    @Test
    public void test_getTotalCount_failOnRetry() throws IOException
    {
        String dataName =  "totalCountFailAllTime";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows(RuntimeException.class, () -> jiraClient.getTotalCount(task));

        // First try + 3 retry_limit
        int expectedInvocation = 3 + 1;
        verify(jiraClient, times(expectedInvocation)).createHttpClient();
        verify(statusLine, times(expectedInvocation)).getStatusCode();
    }

    @Test
    public void test_getTotalCount_doNotRetryOn400Status() throws IOException
    {
        String dataName =  "totalCountFail400";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows(RuntimeException.class, () -> jiraClient.getTotalCount(task));

        // No retry
        int expectedInvocation = 1;
        verify(jiraClient, times(expectedInvocation)).createHttpClient();
        verify(statusLine, times(expectedInvocation)).getStatusCode();
    }

    @Test
    public void test_getTotalCount_retryOnIOException() throws IOException
    {
        when(client.execute(Mockito.any())).thenThrow(new IOException("test exeception"));

        assertThrows(RuntimeException.class, () -> jiraClient.getTotalCount(task));

        // First try + 3 retry_limit
        int expectedInvocation = 3 + 1;
        verify(jiraClient, times(expectedInvocation)).createHttpClient();
        // getStatusCode is not triggered
        verify(statusLine, times(0)).getStatusCode();
    }

    @Test
    public void test_searchIssues() throws IOException
    {
        String dataName =  "searchIssuesSuccess";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();

        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        List<Issue> issues = jiraClient.searchIssues(task, 0, 50);
        assertEquals(issues.size(), 2);
    }

    @Test
    public void test_searchIssues_failJql() throws IOException
    {
        String dataName =  "searchIssuesFailJql";
        JsonObject messageResponse = data.get(dataName).getAsJsonObject();

        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows(ConfigException.class, () -> jiraClient.searchIssues(task, 0, 50));
    }
}
