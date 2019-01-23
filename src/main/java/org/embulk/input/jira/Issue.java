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
import java.util.stream.StreamSupport;

public class Issue
{
    private JsonObject flatten;
    private JsonObject json;

    public Issue(JsonObject original)
    {
        this.json = original;
    }

    public JsonElement fetchValue(String path)
    {
        List<String> keys = new ArrayList<>(Arrays.asList(path.split("\\.")));
        return fetch(json, keys);
    }

    private JsonElement fetch(JsonElement json, List<String> keys)
    {
        if (json == null || json.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        else if (keys.isEmpty() || (json.isJsonArray() && json.getAsJsonArray().size() == 0)) {
            return json;
        }
        String key = keys.get(0);
        keys.remove(0);
        if (json.isJsonArray()) {
            JsonArray arrays = new JsonArray();
            StreamSupport.stream(json.getAsJsonArray().spliterator(), false)
                        .forEach(obj -> {
                            if (obj.isJsonObject()) {
                                arrays.add(obj.getAsJsonObject().get(key));
                            }
                            else {
                                arrays.add(obj);
                            }
                        });
            return fetch(arrays, keys);
        }
        else {
            return fetch(json.getAsJsonObject().get(key), keys);
        }
    }

    public void toRecord()
    {
        flatten = new JsonObject();
        manipulatingFlattenJson(json, "");
    }

    public JsonObject getFlatten()
    {
        return flatten;
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
                        new JsonPrimitive("String value"));
            }
        }
        else if (in.isJsonPrimitive()) {
            flatten.add(prefix, in.getAsJsonPrimitive());
        }
        else {
            flatten.add(prefix, JsonNull.INSTANCE);
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
        }
        else {
            for (String key : heuristic) {
                JsonElement value = json.get(key);
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
