package org.embulk.input.jira.util;

import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.input.jira.JiraInputPlugin.PluginTask;
import org.embulk.input.jira.TestHelpers;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class JiraUtilTest
{
    @Test
    public void test_calculateTotalPage()
    {
        int resultPerPage = 50;
        int expected = 0;
        int totalCount = 0;
        int actual = JiraUtil.calculateTotalPage(totalCount, resultPerPage);
        assertEquals(expected, actual);

        expected = 1;
        totalCount = resultPerPage - 1;
        actual = JiraUtil.calculateTotalPage(totalCount, resultPerPage);
        assertEquals(expected, actual);

        expected = 1;
        totalCount = resultPerPage;
        actual = JiraUtil.calculateTotalPage(totalCount, resultPerPage);
        assertEquals(expected, actual);

        expected = 2;
        totalCount = resultPerPage + 1;
        actual = JiraUtil.calculateTotalPage(totalCount, resultPerPage);
        assertEquals(expected, actual);
    }

    @Test
    public void test_buildPermissionUrl()
    {
        String url = "https://example.com";
        String expected = "https://example.com/rest/api/latest/myself";
        String actual = JiraUtil.buildPermissionUrl(url);
        assertEquals(expected, actual);

        url = "https://example.com/";
        expected = "https://example.com/rest/api/latest/myself";
        actual = JiraUtil.buildPermissionUrl(url);
        assertEquals(expected, actual);

        url = "https://example.com//";
        expected = "https://example.com//rest/api/latest/myself";
        actual = JiraUtil.buildPermissionUrl(url);
        assertEquals(expected, actual);

        url = "https://example.com/sub/subsub";
        expected = "https://example.com/sub/subsub/rest/api/latest/myself";
        actual = JiraUtil.buildPermissionUrl(url);
        assertEquals(expected, actual);
    }

    @Test
    public void test_buildSearchUrl() throws IOException
    {
        PluginTask task = TestHelpers.config().loadConfig(PluginTask.class);
        int startAt = 0;
        int maxResults = 50;
        String expected = "https://example.com/rest/api/latest/search?jql=project+%3D+example&startAt=0&maxResults=50&fields=%2Aall";
        String actual = JiraUtil.buildSearchUrl(task, startAt, maxResults);
        assertEquals(expected, actual);
    }

    @Test
    public void test_validateTaskConfig() throws IOException
    {
        // Happy case
        ConfigSource configSource = TestHelpers.config();
        PluginTask task = configSource.loadConfig(PluginTask.class);
        Exception exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNull(exception);

        // empty username
        configSource = TestHelpers.config();
        configSource.set("username", "");
        task = configSource.loadConfig(PluginTask.class);
        exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ConfigException);
        assertEquals("Username or email could not be empty", exception.getMessage());

        // empty password
        configSource = TestHelpers.config();
        configSource.set("password", "");
        task = configSource.loadConfig(PluginTask.class);
        exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ConfigException);
        assertEquals("Password could not be empty", exception.getMessage());

        // empty uri
        configSource = TestHelpers.config();
        configSource.set("uri", "");
        task = configSource.loadConfig(PluginTask.class);
        exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ConfigException);
        assertEquals("JIRA API endpoint could not be empty", exception.getMessage());

        // invalid uri
        configSource = TestHelpers.config();
        configSource.set("uri", "https://not-existed-domain");
        task = configSource.loadConfig(PluginTask.class);
        exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ConfigException);
        assertEquals("JIRA API endpoint is incorrect or not available", exception.getMessage());

        // empty jql
        configSource = TestHelpers.config();
        configSource.set("jql", "");
        task = configSource.loadConfig(PluginTask.class);
        exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ConfigException);
        assertEquals("JQL could not be empty", exception.getMessage());

        // initial_retry_interval_millis = 0
        configSource = TestHelpers.config();
        configSource.set("initial_retry_interval_millis", 0);
        task = configSource.loadConfig(PluginTask.class);
        exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ConfigException);
        assertEquals("Initial retry delay should be equal or greater than 1", exception.getMessage());

        // retry_limit = -1
        configSource = TestHelpers.config();
        configSource.set("retry_limit", -1);
        task = configSource.loadConfig(PluginTask.class);
        exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ConfigException);
        assertEquals("Retry limit should between 0 and 10", exception.getMessage());

        // retry_limit = 100
        configSource = TestHelpers.config();
        configSource.set("retry_limit", 100);
        task = configSource.loadConfig(PluginTask.class);
        exception = null;
        try {
            JiraUtil.validateTaskConfig(task);
        }
        catch (Exception e) {
            exception = e;
        }
        assertNotNull(exception);
        assertTrue(exception instanceof ConfigException);
        assertEquals("Retry limit should between 0 and 10", exception.getMessage());
    }
}
