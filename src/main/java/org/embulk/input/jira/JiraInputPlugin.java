package org.embulk.input.jira;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.util.concurrent.RateLimiter;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.input.helpers.jira.IssueResult;
import org.embulk.input.helpers.jira.IssueTask;
import org.embulk.input.jira.util.JiraUtil;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class JiraInputPlugin
        implements InputPlugin
{
    private static final Logger LOGGER = Exec.getLogger(JiraInputPlugin.class);
    private static final int MAX_RESULTS = 50;

    public interface PluginTask
            extends Task
    {
        // configuration option 1 (required integer)
        @Config("username")
        public String getUsername();

        // configuration option 2 (optional string, null is not allowed)
        @Config("password")
        public String getPassword();

        @Config("uri")
        public String getUri();

        @Config("retry_initial_wait_sec")
        @ConfigDefault("1")
        public int getRetryInitialWaitSec();

        @Config("retry_limit")
        @ConfigDefault("5")
        public int getRetryLimit();

        @Config("jql")
        public String getJQL();

        @Config("columns")
        public SchemaConfig getColumns();

        // For future support of other authentication method
        @Config("auth_method")
        @ConfigDefault("\"basic\"")
        public AuthenticateMethod getAuthMethod();
    }

    @Override
    public ConfigDiff transaction(ConfigSource config,
            InputPlugin.Control control)
    {
        PluginTask task = config.loadConfig(PluginTask.class);

        Schema schema = task.getColumns().toSchema();
        int taskCount = 1;  // number of run() method calls

        return resume(task.dump(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(TaskSource taskSource,
            Schema schema, int taskCount,
            InputPlugin.Control control)
    {
        control.run(taskSource, schema, taskCount);
        return Exec.newConfigDiff();
    }

    @Override
    public void cleanup(TaskSource taskSource,
            Schema schema, int taskCount,
            List<TaskReport> successTaskReports)
    {
    }

    @Override
    public TaskReport run(TaskSource taskSource,
            Schema schema, int taskIndex,
            PageOutput output)
    {
        PluginTask task = taskSource.loadTask(PluginTask.class);
        JiraUtil.validateTaskConfig(task);
        try (JiraRestClient client = JiraUtil.createJiraRestClient(task)) {
            JiraUtil.checkUserCredentials(client, task);
            String jql = task.getJQL();
            int totalCount = JiraUtil.getTotalCount(client, jql);
            int totalPage = JiraUtil.calculateTotalPage(totalCount, MAX_RESULTS);
            LOGGER.info(String.format("Total pages (%d)", totalPage));
            int currentPage = 0;
            while (currentPage < totalPage) {
                LOGGER.info(String.format("Fetching page %d/%d", (currentPage + 1), totalPage));
                List<String> rawIssuesList = JiraUtil.getRawIssues(client, jql, currentPage, MAX_RESULTS);
                searchIssues(client, rawIssuesList);
                currentPage++;
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        // Write your code here :)
        throw new UnsupportedOperationException("JiraInputPlugin.run method is not implemented yet");
    }

    @Override
    public ConfigDiff guess(ConfigSource config)
    {
        return Exec.newConfigDiff();
    }

    private List<Issue> searchIssues(JiraRestClient client, List<String> rawIssuesList) throws InterruptedException, ExecutionException
    {
        List<Issue> result = new ArrayList<>();
        int maximumRunCount = 10;
        int runCount = 0;
        int threadCount = MAX_RESULTS;
        while (runCount < maximumRunCount && !rawIssuesList.isEmpty()) {
            List<String> failResult = new ArrayList<>();
            ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
            RateLimiter rateLimiter = RateLimiter.create(threadCount);
            RestClientException exception = null;
            try {
                List<Future<IssueResult>> allIssueTasks = new ArrayList<>();
                for (String issueKey : rawIssuesList) {
                    allIssueTasks.add(executorService.submit(new IssueTask(client, issueKey, rateLimiter)));
                }

                for (Future<IssueResult> issueTask : allIssueTasks) {
                    IssueResult issueResult = issueTask.get();
                    if (issueResult.getException() == null) {
                        result.add(issueResult.getIssue());
                    }
                    else {
                        failResult.add(issueResult.getIssueKey());
                        exception = issueResult.getException();
                    }
                }
            }
            finally {
                executorService.shutdown();
            }
            runCount++;
            if (runCount == maximumRunCount && exception != null) {
                throw exception;
            }
            if (exception != null) {
                LOGGER.warn("JIRA return error 401 due to overloading API requests. Retrying on failed items only");
                // Sleep current threads for a while if there still errors
                Thread.sleep(1000 * runCount);
            }
            rawIssuesList = failResult;
        }
        return result;
    }
}
