require "jiralicious"
require "parallel"
require "embulk/input/jira_api/issue"
require "timeout"

module Embulk
  module Input
    module JiraApi
      class Client
        PARALLEL_THREAD_COUNT = 50
        SEARCH_TIMEOUT_SECONDS = 5
        SEARCH_ISSUES_TIMEOUT_SECONDS = 60
        DEFAULT_SEARCH_RETRY_TIMES = 10

        def self.setup(&block)
          Jiralicious.configure(&block)
          new
        end

        def search_issues(jql, options={})
          timeout_and_retry(SEARCH_ISSUES_TIMEOUT_SECONDS) do
            issues = search(jql, options).issues_raw
            Parallel.map(issues, in_threads: PARALLEL_THREAD_COUNT) do |issue_raw|
              # https://github.com/dorack/jiralicious/blob/v0.4.0/lib/jiralicious/search_result.rb#L32-34
              issue = Jiralicious::Issue.find(issue_raw["key"])
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
