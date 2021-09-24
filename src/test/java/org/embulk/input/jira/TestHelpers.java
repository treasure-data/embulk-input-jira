package org.embulk.input.jira;

import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.embulk.config.ConfigSource;
import org.embulk.spi.type.Types;
import org.embulk.util.config.units.ColumnConfig;
import org.embulk.util.config.units.SchemaConfig;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;

import static org.embulk.input.jira.JiraInputPlugin.CONFIG_MAPPER_FACTORY;

public final class TestHelpers
{
    private TestHelpers()
    {
    }

    public static JsonObject getJsonFromFile(final String fileName) throws IOException
    {
        final String path = Resources.getResource(fileName).getPath();
        try (JsonReader reader = new JsonReader(new FileReader(path))) {
            final JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        }
    }

    @SuppressWarnings("serial")
    public static ConfigSource config()
    {
        return CONFIG_MAPPER_FACTORY.newConfigSource()
                .set("type", "jira")
                .set("username", "example@example.com")
                .set("password", "XXXXXXXXXXXXXXXXX")
                .set("uri", "https://example.com/")
                .set("jql", "project = example")
                .set("retry_limit", 3)
                .set("columns", new SchemaConfig(new ArrayList<ColumnConfig>()
                {
                    {
                        add(new ColumnConfig("boolean", Types.BOOLEAN, EMPTY_CONFIG_SOURCE));
                        add(new ColumnConfig("long", Types.LONG, EMPTY_CONFIG_SOURCE));
                        add(new ColumnConfig("double", Types.DOUBLE, EMPTY_CONFIG_SOURCE));
                        add(new ColumnConfig("string", Types.STRING, EMPTY_CONFIG_SOURCE));
                        add(new ColumnConfig("date",
                                Types.TIMESTAMP, CONFIG_MAPPER_FACTORY
                                .newConfigSource().set("format", "%Y-%m-%dT%H:%M:%S.%L%z")));
                        add(new ColumnConfig("json", Types.JSON, EMPTY_CONFIG_SOURCE));
                    }
                }));
    }

    public static ConfigSource dynamicSchemaConfig()
    {
        return config().set("dynamic_schema", true);
    }

    private static final ConfigSource EMPTY_CONFIG_SOURCE = CONFIG_MAPPER_FACTORY.newConfigSource();
}
