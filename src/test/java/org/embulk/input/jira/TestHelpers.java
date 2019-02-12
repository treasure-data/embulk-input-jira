package org.embulk.input.jira;

import com.google.common.io.Resources;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

import java.io.FileReader;
import java.io.IOException;

public final class TestHelpers
{
    private TestHelpers() {}

    public static JsonObject getJsonFromFile(String fileName) throws IOException
     {
        String path = Resources.getResource(fileName).getPath();
        try (JsonReader reader = new JsonReader(new FileReader(path))) {
            JsonParser parser = new JsonParser();
            return parser.parse(reader).getAsJsonObject();
        }
    }
}
