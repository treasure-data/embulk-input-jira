require "jiralicious"

module Jira
  class Api
    def self.setup(&block)
      Jiralicious.configure(&block)
      new
    end

    def search(jql)
      Jiralicious.search(jql)
    end
  end
end
