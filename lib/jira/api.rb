require "jiralicious"
require "jira/issue"

module Jira
  class Api
    def self.setup(&block)
      Jiralicious.configure(&block)
      new
    end

    def search_issues(jql)
      search(jql).issues.map do |issue|
        ::Jira::Issue.new(issue)
      end
    end

    def search(jql)
      Jiralicious.search(jql)
    end
  end
end
