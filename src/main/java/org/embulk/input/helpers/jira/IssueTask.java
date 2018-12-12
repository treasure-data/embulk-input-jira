package org.embulk.input.helpers.jira;

import com.atlassian.jira.rest.client.api.JiraRestClient;
import com.atlassian.jira.rest.client.api.RestClientException;
import com.atlassian.jira.rest.client.api.domain.Issue;
import com.google.common.base.Optional;
import com.google.common.util.concurrent.RateLimiter;

import org.embulk.input.jira.util.JiraUtil;

import java.util.concurrent.Callable;
public class IssueTask implements Callable<IssueResult>
{
    private JiraRestClient client = null;
    private RateLimiter rateLimiter = null;
    private String issueKey = null;

    public IssueTask(JiraRestClient client, String issueKey, RateLimiter rateLimiter)
    {
        this.client = client;
        this.rateLimiter = rateLimiter;
        this.issueKey = issueKey;
    }

    @Override
    public IssueResult call() throws Exception
    {
        rateLimiter.acquire(1);
        try {
            Issue issue = JiraUtil.getIssue(client, issueKey).claim();
            return new IssueResult(issueKey, issue, null);
        }
        catch (RestClientException exception) {
            Optional<Integer> statusCode = exception.getStatusCode();
            if (statusCode.isPresent() && statusCode.get().equals(401)) {
                return new IssueResult(issueKey, null, exception);
            }
            throw exception;
        }
    }
}
