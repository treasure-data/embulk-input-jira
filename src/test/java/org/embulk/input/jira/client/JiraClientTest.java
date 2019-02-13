package org.embulk.input.jira.client;

import com.google.gson.JsonObject;

import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.entity.StringEntity;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.input.jira.TestHelpers;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

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
    public void test_checkUserCredentials_credentialSuccess() throws IOException
    {
        String testName =  "credentialSuccess";
        JsonObject messageResponse = data.get(testName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        jiraClient.checkUserCredentials(task);
    }

    @Test
    public void test_checkUserCredentials_credentialFail400() throws IOException
    {
        String testName =  "credentialFail400";
        JsonObject messageResponse = data.get(testName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_credentialFail401() throws IOException
    {
        String testName =  "credentialFail401";
        JsonObject messageResponse = data.get(testName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_credentialFail429() throws IOException
    {
        String testName =  "credentialFail429";
        JsonObject messageResponse = data.get(testName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential due to problem when contacting JIRA API.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_credentialFail500() throws IOException
    {
        String testName =  "credentialFail500";
        JsonObject messageResponse = data.get(testName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential due to problem when contacting JIRA API.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_getTotalCount_totalCountSuccess() throws IOException
    {
        String testName =  "totalCountSuccess";
        JsonObject messageResponse = data.get(testName).getAsJsonObject();
        int statusCode = messageResponse.get("statusCode").getAsInt();
        String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        int totalCount = jiraClient.getTotalCount(task);
        assertEquals(totalCount, messageResponse.get("body").getAsJsonObject().get("total").getAsInt());
    }

    @Test
    public void test_getTotalCount_totalCountFailAllTime() throws IOException
    {
        String testName =  "totalCountFailAllTime";
        JsonObject messageResponse = data.get(testName).getAsJsonObject();
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
    public void test_getTotalCount_totalCountFail400() throws IOException
    {
        String testName =  "totalCountFail400";
        JsonObject messageResponse = data.get(testName).getAsJsonObject();
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
    public void test_getTotalCount_totalCountWithIOException() throws IOException
    {
        when(client.execute(Mockito.any())).thenThrow(new IOException("test exeception"));

        assertThrows(RuntimeException.class, () -> jiraClient.getTotalCount(task));

        // First try + 3 retry_limit
        int expectedInvocation = 3 + 1;
        verify(jiraClient, times(expectedInvocation)).createHttpClient();
        // getStatusCode is not triggered
        verify(statusLine, times(0)).getStatusCode();
    }
}
