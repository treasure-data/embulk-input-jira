package org.embulk.input.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import org.embulk.config.ConfigDiff;
import org.embulk.config.ConfigException;
import org.embulk.config.ConfigSource;
import org.embulk.config.TaskReport;
import org.embulk.config.TaskSource;
import org.embulk.input.jira.client.JiraClient;
import org.embulk.input.jira.util.JiraUtil;
import org.embulk.spi.Exec;
import org.embulk.spi.InputPlugin;
import org.embulk.spi.PageBuilder;
import org.embulk.spi.PageOutput;
import org.embulk.spi.Schema;
import org.embulk.util.config.Config;
import org.embulk.util.config.ConfigDefault;
import org.embulk.util.config.ConfigMapper;
import org.embulk.util.config.ConfigMapperFactory;
import org.embulk.util.config.Task;
import org.embulk.util.config.TaskMapper;
import org.embulk.util.config.units.ColumnConfig;
import org.embulk.util.config.units.SchemaConfig;
import org.embulk.util.guess.SchemaGuess;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import static org.embulk.input.jira.Constant.GUESS_RECORDS_COUNT;
import static org.embulk.input.jira.Constant.MAX_RESULTS;
import static org.embulk.input.jira.Constant.PREVIEW_RECORDS_COUNT;

public class JiraInputPlugin
        implements InputPlugin
{
    private static final Logger LOGGER = LoggerFactory.getLogger(JiraInputPlugin.class);
    @VisibleForTesting
    public static final ConfigMapperFactory CONFIG_MAPPER_FACTORY = ConfigMapperFactory
            .builder()
            .addDefaultModules()
            .build();
    @VisibleForTesting
    public static final ConfigMapper CONFIG_MAPPER = CONFIG_MAPPER_FACTORY.createConfigMapper();
    private static final TaskMapper TASK_MAPPER = CONFIG_MAPPER_FACTORY.createTaskMapper();

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

        @Config("timeout_millis")
        @ConfigDefault("300000")
        int getTimeoutMillis();

        @Config("retry_limit")
        @ConfigDefault("5")
        public int getRetryLimit();

        @Config("jql")
        @ConfigDefault("null")
        public Optional<String> getJQL();

        @Config("dynamic_schema")
        @ConfigDefault("false")
        public boolean getDynamicSchema();

        @Config("columns")
        public SchemaConfig getColumns();

        // For future support of other authentication methods
        @Config("auth_method")
        @ConfigDefault("\"basic\"")
        public AuthenticateMethod getAuthMethod();
    }

    @Override
    public ConfigDiff transaction(final ConfigSource config,
            final InputPlugin.Control control)
    {
        final PluginTask task = CONFIG_MAPPER.map(config, PluginTask.class);
        SchemaConfig schemaConfig = task.getColumns();
        if (task.getDynamicSchema()) {
            final JiraClient jiraClient = getJiraClient();
            final List<ColumnConfig> columns = new ArrayList<>();
            try {
                final List<ConfigDiff> guessedColumns = getGuessedColumns(jiraClient, task);
                for (final ConfigDiff guessedColumn : guessedColumns) {
                    columns.add(new ColumnConfig(CONFIG_MAPPER_FACTORY.newConfigSource().merge(guessedColumn)));
                }
            }
            catch (final ConfigException e) {
                if (!e.getMessage().equals("Could not guess schema due to empty data set")) {
                    throw e;
                }
            }
            schemaConfig = new SchemaConfig(columns);
        }
        final Schema schema = schemaConfig.toSchema();
        final int taskCount = 1;

        return resume(task.toTaskSource(), schema, taskCount, control);
    }

    @Override
    public ConfigDiff resume(final TaskSource taskSource,
            final Schema schema, final int taskCount,
            final InputPlugin.Control control)
    {
        control.run(taskSource, schema, taskCount);
        return CONFIG_MAPPER_FACTORY.newConfigDiff();
    }

    @Override
    public TaskReport run(final TaskSource taskSource,
            final Schema schema, final int taskIndex,
            final PageOutput output)
    {
        final PluginTask task = TASK_MAPPER.map(taskSource, PluginTask.class);
        JiraUtil.validateTaskConfig(task);
        final JiraClient jiraClient = getJiraClient();
        jiraClient.checkUserCredentials(task);
        try (final PageBuilder pageBuilder = getPageBuilder(schema, output)) {
            if (isPreview()) {
                final List<Issue> issues = jiraClient.searchIssues(task, 0, PREVIEW_RECORDS_COUNT);
                issues.forEach(issue -> JiraUtil.addRecord(issue, schema, task, pageBuilder));
            }
            else {
                int currentPage = 0;
                final int totalCount = jiraClient.getTotalCount(task);
                final int totalPage = JiraUtil.calculateTotalPage(totalCount, MAX_RESULTS);
                LOGGER.info(String.format("Total pages (%d)", totalPage));
                while (currentPage < totalPage) {
                    LOGGER.info(String.format("Fetching page %d/%d", (currentPage + 1), totalPage));
                    final List<Issue> issues = jiraClient.searchIssues(task, (currentPage * MAX_RESULTS), MAX_RESULTS);
                    issues.forEach(issue -> JiraUtil.addRecord(issue, schema, task, pageBuilder));
                    currentPage++;
                }
            }
            pageBuilder.finish();
        }
        return CONFIG_MAPPER_FACTORY.newTaskReport();
    }

    @Override
    public ConfigDiff guess(final ConfigSource config)
    {
        // Reset columns in case already have or missing on configuration
        config.set("columns", new ArrayList<>());
        final PluginTask task = CONFIG_MAPPER.map(config, PluginTask.class);
        JiraUtil.validateTaskConfig(task);
        final JiraClient jiraClient = getJiraClient();
        jiraClient.checkUserCredentials(task);
        return CONFIG_MAPPER_FACTORY.newConfigDiff().set("columns", getGuessedColumns(jiraClient, task));
    }

    private List<ConfigDiff> getGuessedColumns(final JiraClient jiraClient, final PluginTask task)
    {
        final List<Issue> issues = jiraClient.searchIssues(task, 0, GUESS_RECORDS_COUNT);
        if (issues.isEmpty()) {
            throw new ConfigException("Could not guess schema due to empty data set");
        }
        final List<ConfigDiff> columns = SchemaGuess.of(CONFIG_MAPPER_FACTORY).fromLinkedHashMapRecords(createGuessSample(issues, getUniqueAttributes(issues)));
        columns.forEach(conf -> conf.remove("index"));
        return columns;
    }

    private SortedSet<String> getUniqueAttributes(final List<Issue> issues)
    {
        final SortedSet<String> uniqueAttributes = new TreeSet<>();
        for (final Issue issue : issues) {
            for (final Entry<String, JsonElement> entry : issue.getFlatten().entrySet()) {
                uniqueAttributes.add(entry.getKey());
            }
        }
        return uniqueAttributes;
    }

    private List<LinkedHashMap<String, Object>> createGuessSample(final List<Issue> issues, final Set<String> uniqueAttributes)
    {
        final List<LinkedHashMap<String, Object>> samples = new ArrayList<>();
        for (final Issue issue : issues) {
            final JsonObject flatten = issue.getFlatten();
            final JsonObject unified = new JsonObject();
            for (final String key : uniqueAttributes) {
                JsonElement value = flatten.get(key);
                if (value == null) {
                    value = JsonNull.INSTANCE;
                }
                unified.add(key, value);
            }
            samples.add(JiraUtil.toLinkedHashMap(unified));
        }
        return samples;
    }

    @VisibleForTesting
    public PageBuilder getPageBuilder(final Schema schema, final PageOutput output)
    {
        return Exec.getPageBuilder(Exec.getBufferAllocator(), schema, output);
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
    @Override
    public void cleanup(final TaskSource taskSource, final Schema schema, final int taskCount, final List<TaskReport> successTaskReports)
    {}
}
