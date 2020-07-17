package org.embulk.input.jira;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class IssueTest
{
    private static JsonObject flattenData;
    private static JsonObject flattenExpected;
    private static JsonObject issueGet;
    private static JsonObject issueGetExpected;
    @BeforeClass
    public static void setUp() throws IOException
    {
        flattenData = TestHelpers.getJsonFromFile("issue_flatten.json");
        flattenExpected = TestHelpers.getJsonFromFile("issue_flatten_expected.json");
        issueGet = TestHelpers.getJsonFromFile("issue_get.json");
        issueGetExpected = TestHelpers.getJsonFromFile("issue_get_expected.json");
    }

    @Test
    public void test_toRecord_simple()
    {
        String testName = "simple";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_toRecord_twoLevels()
    {
        String testName = "twoLevels";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_toRecord_threeLevels()
    {
        String testName = "threeLevels";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_toRecord_threeLevelsWithoutKeys()
    {
        String testName = "threeLevelsWithoutKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_toRecord_threeLevelsWithKeys()
    {
        String testName = "threeLevelsWithKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_toRecord_threeLevelsWithNullKeys()
    {
        String testName = "threeLevelsWithNullKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_toRecord_arrayWithAllJsonObjectWithSameKeysAndEmptyObject()
    {
        String testName = "arrayWithAllJsonObjectWithSameKeysAndEmptyObject";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_toRecord_arrayWithAllJsonObjectWithSameKeysAndNotEmptyObject()
    {
        String testName = "arrayWithAllJsonObjectWithSameKeysAndNotEmptyObject";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_toRecord_arrayWithAllJsonObjectWithoutSameKeys()
    {
        String testName = "arrayWithAllJsonObjectWithoutSameKeys";
        Issue issue = new Issue(flattenData.get(testName).getAsJsonObject());
        JsonObject expected = flattenExpected.get(testName).getAsJsonObject();
        assertEquals(expected, issue.getFlatten(true));
    }

    @Test
    public void test_getValue_primitive()
    {
        String testName = "primitive";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_string()
    {
        String testName = "string";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_emptyObject()
    {
        String testName = "emptyObject";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_object()
    {
        String testName = "object";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_null()
    {
        String testName = "null";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_notExisted()
    {
        String testName = "notExisted";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_notExisted2Levels()
    {
        String testName = "notExisted2Levels";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_emptyArray()
    {
        String testName = "emptyArray";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_primitiveArray()
    {
        String testName = "primitiveArray";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_nestedPrimitive()
    {
        String testName = "nestedPrimitive";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_nestedObject()
    {
        String testName = "nestedObject";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_nestedArray()
    {
        String testName = "nestedArray";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_3levelsArraysNull()
    {
        String testName = "3levelsArraysNull";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_allJsonObjectArrayPrimitive()
    {
        String testName = "allJsonObjectArrayPrimitive";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_allJsonObjectArrayString()
    {
        String testName = "allJsonObjectArrayString";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_allJsonObjectArrayOnly1()
    {
        String testName = "allJsonObjectArrayOnly1";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }

    @Test
    public void test_getValue_allJsonObjectArrayOnly2()
    {
        String testName = "allJsonObjectArrayOnly2";
        Issue issue = new Issue(issueGet);
        String path = issueGetExpected.get(testName).getAsString();
        JsonElement expected = issueGetExpected.get(testName + "Result");
        assertEquals(expected, issue.getValue(path));
    }
}
