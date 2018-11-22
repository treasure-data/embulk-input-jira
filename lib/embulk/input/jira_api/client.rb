require "jiralicious"
require "parallel"
require "limiter"
require "embulk/input/jira_api/issue"
require "timeout"

module Embulk
  module Input
    module JiraApi
      class Client
        MAX_RATE_LIMIT = 50
        MIN_RATE_LIMIT = 2
        # Normal http request timeout is 300s
        SEARCH_ISSUES_TIMEOUT_SECONDS = 300
        DEFAULT_SEARCH_RETRY_TIMES = 10

        def initialize
          @rate_limiter = Limiter::RateQueue.new(MAX_RATE_LIMIT, interval: 2)
        end

        def self.setup(&block)
          Jiralicious.configure(&block)
          new
        end

        def search_issues(jql, options={})
          issues_raw = search(jql, options).issues_raw
          # Maximum number of issues to retrieve is 50
          rate_limit = MAX_RATE_LIMIT
          success_items = []
          fail_items = []
          error_object = nil
          timeout_and_retry(SEARCH_ISSUES_TIMEOUT_SECONDS * MAX_RATE_LIMIT ) do
            retry_count = 0
            semaphore = Mutex.new
            @rate_limiter = Limiter::RateQueue.new(rate_limit, interval: 2)
            error_object = nil
            while issues_raw.length > 0 && retry_count <= DEFAULT_SEARCH_RETRY_TIMES do
              Parallel.each(issues_raw, in_threads: rate_limit) do |issue_raw|
                # https://github.com/dorack/jiralicious/blob/v0.4.0/lib/jiralicious/search_result.rb#L32-34
                begin
                  issue = find_issue(issue_raw["key"])
                  semaphore.synchronize {
                    success_items.push(JiraApi::Issue.new(issue))
                  }
                rescue MultiJson::ParseError => e
                  html = e.message
                  title = html[%r|<title>(.*?)</title>|, 1]
                  # 401 due to high number of concurrent requests with current account
                  # The number of concurrent requests is not fixed by every account
                  # Hence catch the error item and retry later
                  raise title if title != "Unauthorized (401)"
                  semaphore.synchronize {
                    fail_items.push(issue_raw)
                    error_object = e
                  }
                end
              end
              retry_count += 1
              rate_limit = calculate_rate_limit(rate_limit, issues_raw.length, fail_items.length, retry_count)
              issues_raw = fail_items
              fail_items = []
              raise error_object if retry_count > DEFAULT_SEARCH_RETRY_TIMES && !error_object.nil?
              # Sleep after some seconds for JIRA API perhaps under the overload
              sleep retry_count if fail_items.length > 0
            end
            success_items
          end
        end

        def search(jql, options={})
          timeout_and_retry(SEARCH_ISSUES_TIMEOUT_SECONDS) do
            Jiralicious.search(jql, options)
          end
        end

        def total_count(jql)
          search(jql, max_results: 1).num_results
        end

        def check_user_credential(username)
          Jiralicious::User.search(username)
        rescue Jiralicious::JqlError, Jiralicious::AuthenticationError, Jiralicious::NotLoggedIn, Jiralicious::InvalidLogin => e
          raise Embulk::ConfigError.new(e.message)
        rescue ::SocketError => e
          # wrong `uri` option given
          raise Embulk::ConfigError.new(e.message)
        rescue MultiJson::ParseError => e
          html = e.message
          title = html[%r|<title>(.*?)</title>|, 1] #=> e.g. "Unauthorized (401)"
          raise ConfigError.new("Can not authorize with your credential.") if title == 'Unauthorized (401)'
        end

        # Calculate rate limit based on previous run result
        # Return 2 MIN_RATE_LIMIT in case turning from the 5th times or success_items is less than 2
        # Otherwise return the min number between fail_items, success_items and current_limit
        def calculate_rate_limit(current_limit, all_items, fail_items, times)
          success_items = all_items - fail_items
          return MIN_RATE_LIMIT if times >= DEFAULT_SEARCH_RETRY_TIMES/2 || success_items < MIN_RATE_LIMIT
          return [fail_items, success_items, current_limit].min
        end

        private

        def timeout_and_retry(wait, retry_times = DEFAULT_SEARCH_RETRY_TIMES, &block)
          count = 0
          begin
            Timeout.timeout(wait) do
              yield
            end
          rescue Jiralicious::JqlError, Jiralicious::AuthenticationError, Jiralicious::NotLoggedIn, Jiralicious::InvalidLogin => e
            raise Embulk::ConfigError.new(e.message)
          rescue ::SocketError => e
            # wrong `uri` option given
            raise Embulk::ConfigError.new(e.message)
          rescue MultiJson::ParseError => e
            # same as this Mailchimp plugin issue: https://github.com/treasure-data/embulk-output-mailchimp/issues/10
            # (a) JIRA returns error as HTML, but HTTParty try to parse it as JSON.
            # And (b) `search_issues` method has race-condition bug. If it occurred, MultiJson::ParseError raised too.
            html = e.message
            title = html[%r|<title>(.*?)</title>|, 1] #=> e.g. "Unauthorized (401)"
            raise title if title == "Atlassian Cloud Notifications - Page Unavailable"
            count += 1
            raise title.nil? ? "Unknown Error" : title if count > retry_times
            Embulk.logger.warn "JIRA returns error: #{title}."
            sleep count
            retry
          rescue Timeout::Error => e
            count += 1
            raise e if count > retry_times
            Embulk.logger.warn "Time out error."
            sleep count # retry after some seconds for JIRA API perhaps under the overload
            retry
          end
        end

        def find_issue(issue_key)
          @rate_limiter.shift
          Jiralicious::Issue.find(issue_key)
        end
      end
    end
  end
end
