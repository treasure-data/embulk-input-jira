require "jiralicious"
require "parallel"
require "embulk/input/jira_api/issue"
require "timeout"

module Embulk
  module Input
    module JiraApi
      class Client
        MAX_CONCURRENT_REQUESTS = 50
        # Normal http request timeout is 300s
        SEARCH_ISSUES_TIMEOUT_SECONDS = 300
        DEFAULT_SEARCH_RETRY_TIMES = 10

        def self.setup(&block)
          Jiralicious.configure(&block)
          new
        end

        def find_at_key(array, key)
          
        end

        def search_issues(jql, options={})
          parallel_threads = MAX_CONCURRENT_REQUESTS
          # Maximum time to wait is (300 * maximum_number_of_request / parallel_threads)
          timeout_and_retry(SEARCH_ISSUES_TIMEOUT_SECONDS * MAX_CONCURRENT_REQUESTS ) do
            issues_raw = search(jql, options).issues_raw
            # TODO: below code has race-conditon.
            success_items = []
            fail_items = []
            errors = []
            search_issue_count = 0
            
            while issues_raw.length > 0 && search_issue_count <= DEFAULT_SEARCH_RETRY_TIMES do
              search_results = Parallel.map(issues_raw, in_threads: parallel_threads) do |issue_raw|
                # https://github.com/dorack/jiralicious/blob/v0.4.0/lib/jiralicious/search_result.rb#L32-34
                
                begin
                  issue = Jiralicious::Issue.find(issue_raw["key"])
                  {
                    :error => nil,
                    :result => JiraApi::Issue.new(issue)
                  }
                rescue MultiJson::ParseError => e
                  html = e.message
                  title = html[%r|<title>(.*?)</title>|, 1] #=> e.g. "Unauthorized (401)"
                  raise title if title != "Unauthorized (401)"
                  {
                    :error => e,
                    :issue_raw => issue_raw,
                    :result => nil
                  }
                end
              end
              search_issue_count += 1
              for issue_result in search_results do
                if !issue_result[:error].nil? 
                  fail_items.push(issue_result[:issue_raw])
                  errors.push(issue_result[:error])
                else
                  success_items.push(issue_result[:result])
                end
              end
              raise errors[0] if search_issue_count > DEFAULT_SEARCH_RETRY_TIMES && errors.length > 0
              # Turning number of parallel threads based on number of success and failure items
              # Minumum is 2
              number_of_success_item = issues_raw.length - fail_items.length
              new_parallel_threads = 0;
              if number_of_success_item < 2
                new_parallel_threads = 2
              elsif number_of_success_item > fail_items.length
                new_parallel_threads = fail_items.length
              else
                new_parallel_threads = number_of_success_item
              end
              # Add limit for new parallel_threads to not greater than 2 times the current parallel threads
              parallel_threads = (new_parallel_threads > parallel_threads * 2) ? parallel_threads * 2 : new_parallel_threads
              issues_raw = fail_items
              fail_items = []
              errors = []
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
      end
    end
  end
end
