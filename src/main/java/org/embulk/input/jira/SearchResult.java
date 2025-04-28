package org.embulk.input.jira;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

public class SearchResult
{
    private JsonElement issues;
    private String nextPageToken;

    public JsonElement getIssues()
    {
        if (issues == null || issues.isJsonNull()) {
            issues = new JsonArray();
        }
        return issues;
    }

    public String getNextPageToken()
    {
        return nextPageToken;
    }
}
