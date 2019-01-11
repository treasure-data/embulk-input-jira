package org.embulk.input.jira;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class Issue
{
    private JsonObject flatten;
    private JsonObject flatternType;
    private JsonObject json;

    public static final String JSON_PRIMITIVE = "primitive";
    public static final String JSON_OBJECT = "json";
    public static final String JSON_NULL = "null";

    public Issue(JsonObject original)
    {
        this.json = original;
    }

    public void getValues(String prefix)
    {
        List<String> keys = Arrays.asList(prefix.split("."));
        System.out.println(keys);
    }

    public void toRecord()
    {
        flatten = new JsonObject();
        flatternType = new JsonObject();
        manipulatingFlattenJson(json, "");
    }

    public JsonObject getFlatten()
    {
        return flatten;
    }

    public JsonObject getFlattenType()
    {
        return flatternType;
    }

    private void manipulatingFlattenJson(JsonElement in, String prefix)
    {
        if (in.isJsonObject()) {
            JsonObject obj = in.getAsJsonObject();
            // NOTE: If you want to flatten JSON completely, please remove this if and addHeuristicValue
            if (StringUtils.countMatches(prefix, ".") > 1) {
                addHeuristicValue(obj, prefix);
                return;
            }
            for (Entry<String, JsonElement> entry : obj.entrySet()) {
                String key = entry.getKey();
                JsonElement value = entry.getValue();
                manipulatingFlattenJson(value, appendPrefix(prefix, key));
            }
        }
        else if (in.isJsonArray()) {
            JsonArray arrayObj = in.getAsJsonArray();
            boolean isAllJsonObject = arrayObj.size() > 0 ? StreamSupport.stream(arrayObj.spliterator(), false).allMatch(x -> x.isJsonObject()) : false;
            if (isAllJsonObject) {
                Map<String, Integer> occurents = new HashMap<>();
                for (JsonElement element : arrayObj) {
                    JsonObject obj = element.getAsJsonObject();
                    for (Entry<String, JsonElement> entry : obj.entrySet()) {
                        String key = entry.getKey();
                        occurents.merge(key, 1, Integer::sum);
                    }
                }
                JsonObject newObj = new JsonObject();
                for (String key : occurents.keySet()) {
                    if (newObj.get(key) == null) {
                        newObj.add(key, new JsonArray());
                    }
                    StreamSupport.stream(arrayObj.spliterator(), false).forEach(x -> {
                        newObj.get(key).getAsJsonArray().add(x.getAsJsonObject().get(key));
                    });
                }
                manipulatingFlattenJson(newObj, prefix);
            }
            else {
                flatten.add(prefix,
                        new JsonPrimitive(
                                String.format("\"%s\"", String.join(",",
                                StreamSupport.stream(arrayObj.spliterator(), false)
                                .map(element -> {
                                    if (element.isJsonNull()) {
                                        return "null";
                                    }
                                    else if (element.isJsonPrimitive()) {
                                        return element.getAsJsonPrimitive().getAsString();
                                    }
                                    else if (element.isJsonObject()) {
                                        return element.getAsJsonObject().toString();
                                    }
                                    else {
                                        return element.toString();
                                    }
                }).collect(Collectors.toList())))));
                flatternType.add(prefix, new JsonPrimitive(JSON_PRIMITIVE));
            }
        }
        else if (in.isJsonPrimitive()) {
            flatten.add(prefix, in.getAsJsonPrimitive());
            flatternType.add(prefix, new JsonPrimitive(JSON_PRIMITIVE));
        }
        else {
            flatten.add(prefix, JsonNull.INSTANCE);
            flatternType.add(prefix, new JsonPrimitive(JSON_NULL));
        }
    }

    private void addHeuristicValue(JsonObject json, String prefix)
    {
        List<String> keys = Arrays.asList("name", "key", "id");
        List<String> heuristic = new ArrayList<>();
        for (Entry<String, JsonElement> entry : json.entrySet()) {
            String key = entry.getKey();
            JsonElement value = entry.getValue();
            if (keys.contains(key) && !value.isJsonNull()) {
                heuristic.add(key);
            }
        }
        if (heuristic.isEmpty()) {
            flatten.add(prefix, new JsonPrimitive(json.toString()));
            flatternType.add(prefix, new JsonPrimitive(JSON_OBJECT));
        }
        else {
            for (String key : heuristic) {
                JsonElement value = json.get(key);
                if (value.isJsonPrimitive()) {
                    flatternType.add(appendPrefix(prefix, key), new JsonPrimitive(JSON_PRIMITIVE));
                }
                else {
                    flatternType.add(appendPrefix(prefix, key), new JsonPrimitive(JSON_OBJECT));
                }
                flatten.add(appendPrefix(prefix, key), value);
            }
        }
    }

    private String appendPrefix(String prefix, String key)
    {
        if (prefix.isEmpty()) {
            return key;
        }
        else {
            return prefix.concat(".".concat(key));
        }
    }
}
