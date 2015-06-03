require "jiralicious"
require "jira/issue"
require "timeout"

module Jira
  class Api
    SEARCH_TIMEOUT = 60
    SEARCH_RETRY_TIMES = 10

    def self.setup(&block)
      Jiralicious.configure(&block)
      new
    end

    def search_issues(jql, options={})
      search(jql, options).issues.map do |issue|
        ::Jira::Issue.new(issue)
      end
    end

    def search(jql, options={})
      times = 1
      begin
        Timeout.timeout(SEARCH_TIMEOUT) do
          Jiralicious.search(jql, options)
        end
      rescue Timeout::Error
        times += 1
        sleep times # retry after some seconds for JIRA API perhaps under the overload
        raise "JIRA API was too many timeouts" if times > SEARCH_RETRY_TIMES
        retry
      end
    end

    def total_count(jql)
      search(jql, max_results: 1).num_results
    end
  end
end
