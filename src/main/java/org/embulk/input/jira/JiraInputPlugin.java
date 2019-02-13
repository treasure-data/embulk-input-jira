package org.embulk.input.jira;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;

import org.embulk.config.Config;
import org.embulk.config.ConfigDefault;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigSource;
import org.embulk.config.Task;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.exec.GuessExecutor;
import org.embulk.input.jira.client.JiraClient;
import org.embulk.input.jira.util.JiraUtil;
import org.embulk.spi.Buffer;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.spi.SchemaConfig;
import org.slf4j.Logger;

import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.embulk.input.jira.Constant.GUESS_BUFFER_SIZE;
import static org.embulk.input.jira.Constant.GUESS_RECORDS_COUNT;
import static org.embulk.input.jira.Constant.MAX_RESULTS;
import static org.embulk.input.jira.Constant.PREVIEW_RECORDS_COUNT;

public class JiraInputPlugin
        implements InputPlugin
{
    private static final Logger LOGGER = Exec.getLogger(JiraInputPlugin.class);

    public interface PluginTask
            extends Task
    {
        @Config("username")
        public String getUsername();

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
        int taskCount = 1;

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
        try (final PageBuilder pageBuilder = getPageBuilder(schema, output)) {
            if (isPreview()) {
                List<Issue> issues = jiraClient.searchIssues(task, 0, PREVIEW_RECORDS_COUNT);
                for (Issue issue : issues) {
                    issue.toRecord();
                    JiraUtil.addRecord(issue, schema, task, pageBuilder);
                }
            }
            else {
                int currentPage = 0;
                int totalCount = jiraClient.getTotalCount(task);
                int totalPage = JiraUtil.calculateTotalPage(totalCount, MAX_RESULTS);
                LOGGER.info(String.format("Total pages (%d)", totalPage));
                while (currentPage < totalPage) {
                    LOGGER.info(String.format("Fetching page %d/%d", (currentPage + 1), totalPage));
                    List<Issue> issues = jiraClient.searchIssues(task, (currentPage * MAX_RESULTS), MAX_RESULTS);
                    for (Issue issue : issues) {
                        issue.toRecord();
                        JiraUtil.addRecord(issue, schema, task, pageBuilder);
                    }
                    currentPage++;
                }
            }
            pageBuilder.finish();
        }
        return Exec.newTaskReport();
    }

    @Override
    public ConfigDiff guess(ConfigSource config)
    {
        // Reset columns in case already have or missing on configuration
        config.set("columns", new ObjectMapper().createArrayNode());
        ConfigSource guessConfig = createGuessConfig();
        GuessExecutor guessExecutor = getGuessExecutor();
        PluginTask task = config.loadConfig(PluginTask.class);
        JiraUtil.validateTaskConfig(task);
        JiraClient jiraClient = getJiraClient();
        jiraClient.checkUserCredentials(task);
        List<Issue> issues = jiraClient.searchIssues(task, 0, GUESS_RECORDS_COUNT);
        issues.stream().forEach(issue -> issue.toRecord());
        Set<String> uniqAtrribtes = getUniqueAttributes(issues);
        JsonArray samples = createSamples(issues, uniqAtrribtes);
        Buffer sample = Buffer.copyOf(samples.toString().getBytes());
        JsonNode columns = guessExecutor.guessParserConfig(sample, Exec.newConfigSource(), guessConfig).getObjectNode().get("parser").get("columns");
        ConfigDiff configDiff = Exec.newConfigDiff();
        configDiff.set("columns", columns);
        return configDiff;
    }

    private ConfigSource createGuessConfig()
    {
        ConfigSource configSource = Exec.newConfigSource();
        configSource.set("guess_plugins", new ObjectMapper().createArrayNode().add("jsonpath"));
        configSource.set("guess_sample_buffer_bytes", GUESS_BUFFER_SIZE);
        return configSource;
    }

    private SortedSet<String> getUniqueAttributes(List<Issue> issues)
    {
        SortedSet<String> uniqAttributes = new TreeSet<>();
        issues.stream()
        .forEach(issue -> {
            for (Entry<String, JsonElement> entry : issue.getFlatten().entrySet()) {
                uniqAttributes.add(entry.getKey());
            }
        });
        return uniqAttributes;
    }

    private JsonArray createSamples(List<Issue> issues, Set<String> uniqAtrribtes)
    {
        JsonArray samples = new JsonArray();
        issues.stream()
        .forEach(issue -> {
            JsonObject flatten = issue.getFlatten();
            JsonObject unified = new JsonObject();
            for (String key : uniqAtrribtes) {
                JsonElement value = flatten.get(key);
                if (value == null) {
                    value = JsonNull.INSTANCE;
                }
                unified.add(key, value);
            }
            samples.add(unified);
        });
        return samples;
    }

    @VisibleForTesting
    public GuessExecutor getGuessExecutor()
    {
        return Exec.getInjector().getInstance(GuessExecutor.class);
    }

    @VisibleForTesting
    public PageBuilder getPageBuilder(Schema schema, PageOutput output)
    {
        return new PageBuilder(Exec.getBufferAllocator(), schema, output);
    }

    @VisibleForTesting
    public boolean isPreview()
    {
        return Exec.isPreview();
    }

    @VisibleForTesting
    public JiraClient getJiraClient()
    {
        return new JiraClient();
    }
}
