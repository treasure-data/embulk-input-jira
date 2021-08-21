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
    private final JsonObject json;

    public Issue(final JsonObject original)
    {
        this.json = original;
    }

    public JsonElement getValue(final String path)
    {
        final List<String> keys = new ArrayList<>(Arrays.asList(path.split("\\.")));
        return get(json, keys);
    }

    private JsonElement get(final JsonElement json, final List<String> keys)
    {
        if (json == null || json.isJsonNull()) {
            return JsonNull.INSTANCE;
        }
        else if (keys.isEmpty() || (json.isJsonArray() && json.getAsJsonArray().size() == 0)) {
            return json;
        }
        final String key = keys.get(0);
        keys.remove(0);
        if (json.isJsonArray()) {
            final JsonArray arrays = new JsonArray();
            for (final JsonElement elem : json.getAsJsonArray()) {
                if (elem.isJsonObject()) {
                    arrays.add(elem.getAsJsonObject().get(key));
                }
                else {
                    arrays.add(elem);
                }
            }
            return get(arrays, keys);
        }
        else {
            return get(json.getAsJsonObject().get(key), keys);
        }
    }

    public synchronized JsonObject getFlatten()
    {
        if (flatten == null) {
            flatten = new JsonObject();
            manipulatingFlattenJson(json, "");
        }
        return flatten;
    }

    private void manipulatingFlattenJson(final JsonElement in, final String prefix)
    {
        if (in.isJsonObject()) {
            final JsonObject obj = in.getAsJsonObject();
            // NOTE: If you want to flatten JSON completely, please remove this if and addHeuristicValue
            if (StringUtils.countMatches(prefix, ".") > 1) {
                addHeuristicValue(obj, prefix);
                return;
            }
            if (obj.entrySet().isEmpty()) {
                flatten.add(prefix, obj);
            }
            else {
                for (final Entry<String, JsonElement> entry : obj.entrySet()) {
                    final String key = entry.getKey();
                    final JsonElement value = entry.getValue();
                    manipulatingFlattenJson(value, appendPrefix(prefix, key));
                }
            }
        }
        else if (in.isJsonArray()) {
            final JsonArray arrayObj = in.getAsJsonArray();
            final boolean isAllJsonObject = arrayObj.size() > 0 && StreamSupport.stream(arrayObj.spliterator(), false).allMatch(JsonElement::isJsonObject);
            if (isAllJsonObject) {
                final Map<String, Integer> occurents = new HashMap<>();
                for (final JsonElement element : arrayObj) {
                    final JsonObject obj = element.getAsJsonObject();
                    for (final Entry<String, JsonElement> entry : obj.entrySet()) {
                        final String key = entry.getKey();
                        occurents.merge(key, 1, Integer::sum);
                    }
                }
                final JsonObject newObj = new JsonObject();
                for (final String key : occurents.keySet()) {
                    newObj.add(key, new JsonArray());
                    for (final JsonElement elem : arrayObj) {
                        newObj.get(key).getAsJsonArray().add(elem.getAsJsonObject().get(key));
                    }
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

    private void addHeuristicValue(final JsonObject json, final String prefix)
    {
        final List<String> keys = Arrays.asList("name", "key", "id");
        final List<String> heuristic = new ArrayList<>();
        for (final Entry<String, JsonElement> entry : json.entrySet()) {
            final String key = entry.getKey();
            final JsonElement value = entry.getValue();
            if (keys.contains(key) && !value.isJsonNull()) {
                heuristic.add(key);
            }
        }
        if (heuristic.isEmpty()) {
            flatten.add(prefix, new JsonPrimitive(json.toString()));
        }
        else {
            for (final String key : heuristic) {
                final JsonElement value = json.get(key);
                flatten.add(appendPrefix(prefix, key), value);
            }
        }
    }

    private String appendPrefix(final String prefix, final String key)
    {
        return prefix.isEmpty() ? key : prefix + "." + key;
    }
}
