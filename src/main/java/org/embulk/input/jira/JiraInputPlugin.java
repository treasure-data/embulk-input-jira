package org.embulk.input.jira;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.input.jira.client.JiraClient;
import org.embulk.input.jira.util.JiraUtil;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.slf4j.Logger;

import java.util.List;

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

        @Config("initial_retry_interval_millis")
        @ConfigDefault("1000")
        int getInitialRetryIntervalMillis();

        @Config("maximum_retry_interval_millis")
        @ConfigDefault("120000")
        int getMaximumRetryIntervalMillis();

        // timeout settings
        @Config("timeout_millis")
        @ConfigDefault("300000")
        int getTimeoutMillis();

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
        JiraClient jiraClient = getJiraClient();
        jiraClient.checkUserCredentials(task);
        int totalCount = jiraClient.getTotalCount(task);
        int totalPage = JiraUtil.calculateTotalPage(totalCount, MAX_RESULTS);
        LOGGER.info(String.format("Total pages (%d)", totalPage));
        int currentPage = 0;
        try (final PageBuilder pageBuilder = new PageBuilder(Exec.getBufferAllocator(), schema, output)) {
            while (currentPage < totalPage) {
                LOGGER.info(String.format("Fetching page %d/%d", (currentPage + 1), totalPage));
                List<Issue> issues = jiraClient.searchIssues(task, (currentPage * MAX_RESULTS), MAX_RESULTS);
                for (Issue issue : issues) {
                    issue.toRecord();
                    JiraUtil.addRecord(issue, schema, task, pageBuilder);
                }
                currentPage++;
            }
            pageBuilder.finish();
        }
        return Exec.newTaskReport();
    }

    private JiraClient getJiraClient()
    {
        return new JiraClient();
    }

    @Override
    public ConfigDiff guess(ConfigSource config)
    {
        return Exec.newConfigDiff();
    }
}
