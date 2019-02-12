package org.embulk.input.jira;

import com.google.gson.JsonObject;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class TestIssue
{
    private static JsonObject flattenData;
    private static JsonObject flattenExpected;
    @BeforeClass
    public static void setUp() throws IOException
    {
        flattenData = TestHelpers.getJsonFromFile("issue_flatten.json");
        flattenExpected = TestHelpers.getJsonFromFile("issue_flatten_expected.json");
    }

    @Test
    public void test_toRecord_simple()
    {
        String testName = "simple";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        issue.toRecord();
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten());
    }

    @Test
    public void test_toRecord_twoLevels()
    {
        String testName = "twoLevels";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        issue.toRecord();
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten());
    }

    @Test
    public void test_toRecord_threeLevels()
    {
        String testName = "threeLevels";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        issue.toRecord();
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten());
    }

    @Test
    public void test_toRecord_threeLevelsWithoutKeys()
    {
        String testName = "threeLevelsWithoutKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        issue.toRecord();
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten());
    }

    @Test
    public void test_toRecord_threeLevelsWithKeys()
    {
        String testName = "threeLevelsWithKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        issue.toRecord();
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten());
    }

    @Test
    public void test_toRecord_threeLevelsWithNullKeys()
    {
        String testName = "threeLevelsWithNullKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        issue.toRecord();
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten());
    }

    @Test
    public void test_toRecord_arrayWithAllJsonObjectWithSameKeys()
    {
        String testName = "arrayWithAllJsonObjectWithSameKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        issue.toRecord();
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten());
    }

    @Test
    public void test_toRecord_arrayWithAllJsonObjectWithoutSameKeys()
    {
        String testName = "arrayWithAllJsonObjectWithoutSameKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        issue.toRecord();
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten());
    }
}
