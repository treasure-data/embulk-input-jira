package org.embulk.input.helpers.jira;

import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;

public class IssueResult
{
    private String issueKey = null;
    private Issue issue = null;
    private RestClientException exception = null;

    public IssueResult(String issueKey, Issue issue, RestClientException exception)
    {
        this.issueKey = issueKey;
        this.issue = issue;
        this.exception = exception;
    }

    public String getIssueKey()
    {
        return issueKey;
    }

    public Issue getIssue()
    {
        return issue;
    }

    public RestClientException getException()
    {
        return exception;
    }
}
