require "perfect_retry"
require "embulk/input/jira_input_plugin_utils"
require "embulk/input/jira_api"

module Embulk
  module Input
    class Jira < InputPlugin
      PER_PAGE = 50
      GUESS_RECORDS_COUNT = 10
      PREVIEW_RECORDS_COUNT = 15

      Plugin.register_input("jira", self)

      def self.transaction(config, &control)
        task = {
          username: config.param(:username, :string),
          password: config.param(:password, :string),
          uri: config.param(:uri, :string),
          jql: config.param(:jql, :string),
        }

        attributes = {}
        columns = config.param(:columns, :array).map do |column|
          name = column["name"]
          type = column["type"].to_sym
          attributes[name] = type
          Column.new(nil, name, type, column["format"])
        end

        task[:attributes] = attributes

        resume(task, columns, 1, &control)
      end

      def self.resume(task, columns, count, &control)
        task_reports = yield(task, columns, count)

        next_config_diff = {}
        return next_config_diff
      end

      def self.guess(config)
        username = config.param(:username, :string)
        password = config.param(:password, :string)
        uri = config.param(:uri, :string)
        jql = config.param(:jql, :string)

        jira = JiraApi::Client.setup do |jira_config|
          # TODO: api_version should be 2 (the latest version)
          # auth_type should be specified from config. (The future task)

          jira_config.username = username
          jira_config.password = password
          jira_config.uri = uri
          jira_config.api_version = "latest"
          jira_config.auth_type = "basic"
        end

        # TODO: we use 0..10 issues to guess config?
        records = jira.search_issues(jql, max_results: GUESS_RECORDS_COUNT).map do |issue|
          issue.to_record
        end

        columns = JiraInputPluginUtils.guess_columns(records)

        guessed_config = {
          "columns" => columns,
        }

        return guessed_config
      end

      def init
        @attributes = task[:attributes]
        @jira = JiraApi::Client.setup do |config|
          config.username = task[:username]
          config.password = task[:password]
          config.uri = task[:uri]
          config.api_version = "latest"
          config.auth_type = "basic"
        end
        @jql = task[:jql]
      end

      def run
        return preview if preview?
        options = {}
        total_count = @jira.total_count(@jql)
        last_page = (total_count.to_f / PER_PAGE).ceil

        0.step(total_count, PER_PAGE).with_index(1) do |start_at, page|
          logger.debug "Fetching #{page} / #{last_page} page"
          @jira.search_issues(@jql, options.merge(start_at: start_at)).each do |issue|
            values = @attributes.map do |(attribute_name, type)|
              JiraInputPluginUtils.cast(issue[attribute_name], type)
            end

            page_builder.add(values)
          end
        end

        page_builder.finish

        task_report = {}
        return task_report
      end

      def self.logger
        Embulk.logger
      end

      def logger
        self.class.logger
      end

      private

      def preview
        logger.debug "For preview mode, JIRA input plugin fetches records at most #{PREVIEW_RECORDS_COUNT}"
        @jira.search_issues(@jql, max_results: PREVIEW_RECORDS_COUNT).each do |issue|
          values = @attributes.map do |(attribute_name, type)|
            JiraInputPluginUtils.cast(issue[attribute_name], type)
          end
          page_builder.add(values)
        end
        page_builder.finish

        task_report = {}
        return task_report
      end

      def preview?
        begin
          # http://www.embulk.org/docs/release/release-0.6.12.html
          org.embulk.spi.Exec.isPreview()
        rescue java.lang.NullPointerException => e
          false
        end
      end
    end
  end
end
