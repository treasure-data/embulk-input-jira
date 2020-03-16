package org.embulk.input.jira;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.guava.GuavaModule;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import org.embulk.config.ConfigLoader;
import org.embulk.config.ConfigSource;
import org.embulk.config.ModelManager;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;

public final class TestHelpers
{
    private TestHelpers() {}

    public static JsonObject getJsonFromFile(final String fileName) throws IOException
     {
        final String path = Resources.getResource(fileName).getPath();
        try (JsonReader reader = new JsonReader(new FileReader(path))) {
            final JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        }
    }

    public static ConfigSource config() throws IOException
    {
        final String path = Resources.getResource("config.yml").getPath();
        final ObjectMapper mapper = new ObjectMapper()
                .registerModule(new GuavaModule())
                .registerModule(new Jdk8Module());
        final ConfigLoader configLoader = new ConfigLoader(new ModelManager(null, mapper));
        return configLoader.fromYamlFile(new File(path));
    }
}
