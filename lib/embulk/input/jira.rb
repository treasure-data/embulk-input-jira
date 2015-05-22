require "jira/api"

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

        attributes = {}
        columns = config.param("columns", :array).map do |column|
          name = column["name"]
          type = column["type"].to_sym
          attributes[name] = type
          Column.new(nil, name, type, column["format"])
        end

        task["attributes"] = attributes

        resume(task, columns, 1, &control)
      end

      def self.resume(task, columns, count, &control)
        commit_reports = yield(task, columns, count)

        next_config_diff = {}
        return next_config_diff
      end

      def self.guess(config)
        # TODO: api_version, and auth_type should be define as constant, or specified from config...

        username = config.param("username", :string)
        password = config.param("password", :string)
        uri = config.param("uri", :string)
        api_version = "latest"
        auth_type = "basic"
        jql = config.param("jql", :string)

        jira = Jira::Api.setup do |jira_config|
          jira_config.username = username
          jira_config.password = password
          jira_config.uri = uri
          jira_config.api_version = api_version
          jira_config.auth_type = auth_type
        end

        # TODO: we use 0..10 issues to guess config?
        records = jira.search_issues(jql)[0..10].map do |issue|
          generate_record(issue.fields)
        end

        columns = Guess::SchemaGuess.from_hash_records(records).map do |c|
          column = {name: c.name, type: c.type}
          column[:format] = c.format if c.format
          column
        end

        guessed_config = {
          "username" => username,
          "password" => password,
          "uri" => uri,
          "api_version" => api_version,
          "auth_type" => auth_type,
          "columns" => columns,
        }

        return guessed_config
      end

      def self.generate_record(fields)
        record = {}
        fields.each_pair do |key, value|
          field_key = key.dup

          if value.is_a?(String)
            field_value = value
          else

            # TODO: refactor...
            if value.is_a?(Hash)
              if value.keys.include?("name")
                field_key << ".name"
                field_value = value["name"]
              elsif value.keys.include?("id")
                field_key << ".id"
                field_value = value["id"]
              else
                field_value = value.to_json.to_s
              end
            else
              field_value = value.to_json.to_s
            end
          end

          record[field_key] = field_value
        end

        record
      end

      def init
        # initialization code:
        @attributes = task["attributes"]
        @jira = Jira::Api.setup do |config|
          config.username = task["username"]
          config.password = task["password"]
          config.uri = task["uri"]
          config.api_version = "latest"
          config.auth_type = :basic
        end
      end

      def run
        @jira.search_issues(task["jql"]).each do |issue|
          values = @attributes.map do |(attribute_name, type)|
            cast(issue[attribute_name], type)
          end

          page_builder.add(values)
        end
        page_builder.finish

        commit_report = {}
        return commit_report
      end

      private

      def cast(value, type)
        return value if value.nil?

        case type.to_sym
        when :long
          Integer(value)
        when :double
          Float(value)
        when :timestamp
          Time.parse(value)
        when :boolean
          !!value
        else
          value.to_s
        end
      end
    end
  end
end
