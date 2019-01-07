package org.embulk.input.jira.util;

public class JiraException extends Exception
{
    private static final long serialVersionUID = -256731723520584046L;
    private final int statusCode;

    public JiraException(int statusCode, String message)
    {
        super(message + ":" + Integer.toString(statusCode));
        this.statusCode = statusCode;
    }

    public int getStatusCode()
    {
        return statusCode;
    }
}
