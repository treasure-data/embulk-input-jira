require "jiralicious"
require "embulk/input/jira_api/issue"
require "timeout"

module Embulk
  module Input
    module JiraApi
      class Api
        SEARCH_TIMEOUT_SECONDS = 5
        SEARCH_ISSUES_TIMEOUT_SECONDS = 60
        DEFAULT_SEARCH_RETRY_TIMES = 10

        def self.setup(&block)
          Jiralicious.configure(&block)
          new
        end

        def search_issues(jql, options={})
          timeout_and_retry(SEARCH_ISSUES_TIMEOUT_SECONDS) do
            search(jql, options).issues.map do |issue|
              JiraApi::Issue.new(issue)
            end
          end
        end

        def search(jql, options={})
          timeout_and_retry(SEARCH_TIMEOUT_SECONDS) do
            Jiralicious.search(jql, options)
          end
        end

        def total_count(jql)
          search(jql, max_results: 1).num_results
        end

        private

        def timeout_and_retry(wait, retry_times = DEFAULT_SEARCH_RETRY_TIMES, &block)
          count = 1
          begin
            Timeout.timeout(wait) do
              yield
            end
          rescue Timeout::Error => e
            count += 1
            sleep count # retry after some seconds for JIRA API perhaps under the overload
            raise e if count > retry_times
            retry
          end
        end
      end
    end
  end
end
