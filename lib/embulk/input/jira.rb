require "jira/api"

module Embulk
  module Input
    class JiraInputPlugin < InputPlugin
      Plugin.register_input("jira", self)

      def self.transaction(config, &control)
        # configuration code:
        attributes = extract_attributes(config.param("attributes", :array))

        task = {
          "username" => config.param("username", :string),
          "password" => config.param("password", :string),
          "uri" => config.param("uri", :string),
          "jql" => config.param("jql", :string),
          "attributes" => attributes,
        }

        columns = attributes.map.with_index do |(attribute_name, type), i|
          Column.new(i, attribute_name, type.to_sym)
        end

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
          fields = {}
          issue.fields.each_pair do |key, value|
            field_key = key.dup
            field_value = value

            if value.is_a?(Hash)
              if value.keys.include?("name")
                field_key << ".name"
                field_value = value["name"]
              else
                field_key << ".id"
                field_value = value["id"]
              end
            end

            fields[field_key] = field_value
          end

          fields
        end

        columns = Guess::SchemaGuess.from_hash_records(records)

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

      def self.extract_attributes(attribute_names)
        unsupported_attributes = attribute_names - Jira::Issue::SUPPORTED_ATTRIBUTES

        unless unsupported_attributes.empty?
          unsupported_attribute_names =
            unsupported_attributes.map {|attr| "'#{attr}'"}.join(', ')

          raise(<<-MESSAGE)
Unsupported Jira attributes is(are) specified.
We support #{Jira::Issue::SUPPORTED_ATTRIBUTE_NAMES}, but your config includes #{unsupported_attribute_names}.
          MESSAGE
        end

        attribute_names.map do |name|
          type = Jira::Issue.detect_attribute_type(name)
          [name, type]
        end
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
          values = @attributes.map do |attribute_name, _|
            issue[attribute_name]
          end
          page_builder.add(values)
        end
        page_builder.finish

        commit_report = {}
        return commit_report
      end
    end
  end
end
