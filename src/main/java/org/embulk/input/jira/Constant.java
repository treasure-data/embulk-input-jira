package org.embulk.input.jira;

public final class Constant
{
    public static final int MAX_RESULTS = 50;
    public static final int MIN_RESULTS = 1;
    public static final int GUESS_RECORDS_COUNT = 50;
    public static final int PREVIEW_RECORDS_COUNT = 10;
    public static final int GUESS_BUFFER_SIZE = 5 * 1024 * 1024;

    public static final String DEFAULT_TIMESTAMP_PATTERN = "%Y-%m-%dT%H:%M:%S.%L%z";

    public static final String CREDENTIAL_URI_PATH = "rest/api/latest/myself";
    public static final String SEARCH_URI_PATH = "rest/api/latest/search";

    private Constant(){}
}
