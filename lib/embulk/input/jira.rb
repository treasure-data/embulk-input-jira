require "jiralicious"

module Embulk
  module Input
    class JiraInputPlugin < InputPlugin
      Plugin.register_input("jira", self)

      def self.transaction(config, &control)
        # configuration code:

        task = {
          "username" => config.param("username", :string),
          "password" => config.param("password", :string),
          "uri" => config.param("uri", :string),
          "jql" => config.param("jql", :string),
        }

        columns = [
          Column.new(0, "summary", :string),
          Column.new(1, "project", :string),
        ]

        resume(task, columns, 1, &control)
      end

      def self.resume(task, columns, count, &control)
        commit_reports = yield(task, columns, count)

        next_config_diff = {}
        return next_config_diff
      end

      # TODO
      #def self.guess(config)
      #  sample_records = [
      #    {"example"=>"a", "column"=>1, "value"=>0.1},
      #    {"example"=>"a", "column"=>2, "value"=>0.2},
      #  ]
      #  columns = Guess::SchemaGuess.from_hash_records(sample_records)
      #  return {"columns" => columns}
      #end

      def init
        # initialization code:
        Jiralicious.configure do |config|
          # Leave out username and password
          config.username = task["username"]
          config.password = task["password"]
          config.uri = task["uri"]
          config.api_version = "latest"
          config.auth_type = :basic
        end

        @jql = task["jql"]
      end

      def run
        Jiralicious.search(@jql).issues.each do |issue|
          field = issue["fields"]
          page_builder.add([field["summary"], field["project"]["key"]])
        end
        page_builder.finish

        commit_report = {}
        return commit_report
      end
    end
  end
end
