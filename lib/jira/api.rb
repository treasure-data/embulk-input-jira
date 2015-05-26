require "jiralicious"
require "jira/issue"

module Jira
  class Api
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
      Jiralicious.search(jql, options)
    end

    def total_count(jql)
      Jiralicious.search(jql).num_results
    end
  end
end
