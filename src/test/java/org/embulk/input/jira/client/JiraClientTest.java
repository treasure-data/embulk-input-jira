package org.embulk.input.jira.client;

import com.google.gson.JsonObject;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.embulk.EmbulkTestRuntime;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.input.jira.Issue;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.input.jira.TestHelpers;
import org.embulk.input.jira.util.JiraUtil;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;
import java.util.List;

import static org.embulk.input.jira.JiraInputPlugin.CONFIG_MAPPER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.mockito.Mockito.when;

public class JiraClientTest
{
    @Rule
    public EmbulkTestRuntime runtime = new EmbulkTestRuntime();
    private JiraClient jiraClient;
    private PluginTask task;

    private final CloseableHttpClient client = Mockito.mock(CloseableHttpClient.class);
    private CloseableHttpResponse response = Mockito.mock(CloseableHttpResponse.class);
    private final StatusLine statusLine = Mockito.mock(StatusLine.class);
    private JsonObject data;

    @Before
    public void setUp() throws IOException
    {
        if (jiraClient == null) {
            jiraClient = Mockito.spy(new JiraClient());
            response = Mockito.mock(CloseableHttpResponse.class);
            task = CONFIG_MAPPER.map(TestHelpers.config(), PluginTask.class);
            data = TestHelpers.getJsonFromFile("jira_client.json");
        }
        when(jiraClient.createHttpClient()).thenReturn(client);
        when(client.execute(Mockito.any())).thenReturn(response);
        when(response.getStatusLine()).thenReturn(statusLine);
    }

    @Test
    public void test_checkUserCredentials_success() throws IOException
    {
        final String dataName =  "credentialSuccess";
        final JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        final int statusCode = messageResponse.get("statusCode").getAsInt();
        final String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        jiraClient.checkUserCredentials(task);
    }

    @Test
    public void test_checkUserCredentials_failOn400() throws IOException
    {
        final String dataName =  "credentialFail400";
        final JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        final int statusCode = messageResponse.get("statusCode").getAsInt();
        final String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_failOn401() throws IOException
    {
        final String dataName =  "credentialFail401";
        final JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        final int statusCode = messageResponse.get("statusCode").getAsInt();
        final String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_failOn429() throws IOException
    {
        final String dataName =  "credentialFail429";
        final JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        final int statusCode = messageResponse.get("statusCode").getAsInt();
        final String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential due to problems when contacting JIRA API.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_checkUserCredentials_failOn500() throws IOException
    {
        final String dataName =  "credentialFail500";
        final JsonObject messageResponse = data.get(dataName).getAsJsonObject();
        final int statusCode = messageResponse.get("statusCode").getAsInt();
        final String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows("Could not authorize with your credential due to problems when contacting JIRA API.", ConfigException.class, () -> jiraClient.checkUserCredentials(task));
    }

    @Test
    public void test_searchIssues() throws IOException
    {
        final String dataName =  "searchIssuesSuccess";
        final JsonObject messageResponse = data.get(dataName).getAsJsonObject();

        final int statusCode = messageResponse.get("statusCode").getAsInt();
        final String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        final List<Issue> issues = JiraUtil.fromSearchResult(jiraClient.searchIssues(task, null, 50));
        assertEquals(issues.size(), 2);
    }

    @Test
    public void test_searchIssues_failJql() throws IOException
    {
        final String dataName =  "searchIssuesFailJql";
        final JsonObject messageResponse = data.get(dataName).getAsJsonObject();

        final int statusCode = messageResponse.get("statusCode").getAsInt();
        final String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));

        assertThrows(ConfigException.class, () -> jiraClient.searchIssues(task, null, 50));
    }

    @Test
    public void test_searchIssues_emptyJql() throws IOException
    {
        final String dataName =  "searchIssuesSuccess";
        final JsonObject messageResponse = data.get(dataName).getAsJsonObject();

        final int statusCode = messageResponse.get("statusCode").getAsInt();
        final String body = messageResponse.get("body").toString();

        when(statusLine.getStatusCode()).thenReturn(statusCode);
        when(response.getEntity()).thenReturn(new StringEntity(body));
        ConfigSource config = TestHelpers.config().remove("jql");
        task = CONFIG_MAPPER.map(config, PluginTask.class);

        List<Issue> issues = JiraUtil.fromSearchResult(jiraClient.searchIssues(task, null, 50));
        assertEquals(issues.size(), 2);

        config = TestHelpers.config().set("jql", "");
        task = CONFIG_MAPPER.map(config, PluginTask.class);

        issues = JiraUtil.fromSearchResult(jiraClient.searchIssues(task, null, 50));
        assertEquals(issues.size(), 2);
    }
}
