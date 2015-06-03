require "jiralicious"
require "jira/issue"
require "timeout"

module Jira
  class Api
    SEARCH_TIMEOUT = 5
    SEARCH_ISSUES_TIMEOUT = 60
    SEARCH_RETRY_TIMES = 10

    def self.setup(&block)
      Jiralicious.configure(&block)
      new
    end

    def search_issues(jql, options={})
      timeout_and_retry(SEARCH_ISSUES_TIMEOUT, SEARCH_RETRY_TIMES) do
        search(jql, options).issues.map do |issue|
          ::Jira::Issue.new(issue)
        end
      end
    end

    def search(jql, options={})
      timeout_and_retry(SEARCH_TIMEOUT, SEARCH_RETRY_TIMES) do
        Jiralicious.search(jql, options)
      end
    end

    def total_count(jql)
      search(jql, max_results: 1).num_results
    end

    private

    def timeout_and_retry(wait, retry_times = 10, &block)
      times = 1
      begin
        Timeout.timeout(wait) do
          yield
        end
      rescue Timeout::Error => e
        times += 1
        sleep times # retry after some seconds for JIRA API perhaps under the overload
        raise e if times > retry_times
        retry
      end
    end
  end
end
