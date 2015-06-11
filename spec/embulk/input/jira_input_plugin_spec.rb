require "spec_helper"

describe Embulk::Input::JiraInputPlugin do
  let(:username) { "jira-user" }
  let(:password) { "password" }
  let(:uri) { "http://jira.example/" }
  let(:jql) { "PROJECT=#{project_name}" }
  let(:project_name) { "FOO" }

  describe ".transaction" do
    subject { Embulk::Input::JiraInputPlugin.transaction(config, &control) }

    let(:config) { Object.new } # add mock later
    let(:control) { Proc.new{|task, columns, count| } } # do nothing

    let(:task) do
      {
        "username" => username,
        "password" => password,
        "uri" => uri,
        "jql" => jql,
        "attributes" => {
          "project.key" => :string,
          "comment.total" => :long
        }
      }
    end

    let(:columns) do
      [
        {"name" => "project.key", "type" => "string"},
        {"name" => "comment.total", "type" => "long"}
      ]
    end

    let(:column_structs) do
      [
        Embulk::Column.new(nil, "project.key", :string),
        Embulk::Column.new(nil, "comment.total", :long)
      ]
    end

    before do
      allow(config).to receive(:param).with("username", :string).and_return(username)
      allow(config).to receive(:param).with("password", :string).and_return(password)
      allow(config).to receive(:param).with("uri", :string).and_return(uri)
      allow(config).to receive(:param).with("jql", :string).and_return(jql)
      allow(config).to receive(:param).with("columns", :array).and_return(columns)
    end

    # NOTE: I should check other factor, but i don't know it...
    it "calls .resume method with proper parameters" do
      expect(Embulk::Input::JiraInputPlugin).to receive(:resume).with(task, column_structs, 1, &control)
      subject
    end
  end

  describe ".resume" do
    subject { Embulk::Input::JiraInputPlugin.resume(task, columns, count, &control) }

    let(:task) do
      {
        "username" => username,
        "password" => password,
        "uri" => uri,
        "jql" => jql,
        "attributes" => {
          "project.key" => :string,
          "comment.total" => :long
        }
      }
    end

    let(:columns) do
      [
        Embulk::Column.new(nil, "project.key", :string),
        Embulk::Column.new(nil, "comment.total", :long)
      ]
    end

    let(:count) { 1 }
    let(:control) { Proc.new{|task, columns, count| } } # do nothing

    let(:next_config_diff) { {} }

    it "returns next_config_diff" do
      expect(subject).to eq next_config_diff
    end
  end

  describe ".guess" do
    subject { Embulk::Input::JiraInputPlugin.guess(config) }

    let(:config) { Object.new } # add mock later

    let(:jira_api) { Embulk::Input::Jira::Api.new }
    let(:jira_issues) { [Embulk::Input::Jira::Issue.new(attributes)] }
    let(:attributes) do
      {
        "id" => "100",
        "jira_key" => "FOO-100",
        "fields" =>
        {
          "project" => {
            "name" => project_name,
            "key" => project_name,
          },
          "comment" => {
            "total" => 0,
            "comments" => []
          }
        }
      }
    end

    let(:guessed_config) do
      {
        "columns" => [
          {name: "id", type: :long},
          {name: "key", type: :string},
          {name: "project.name", type: :string},
          {name: "comment", type: :string}
        ]
      }
    end

    before do
      allow(jira_api).to receive(:search_issues).with(jql, max_results: Embulk::Input::JiraInputPlugin::GUESS_RECORDS_COUNT).and_return(jira_issues)

      allow(config).to receive(:param).with("username", :string).and_return(username)
      allow(config).to receive(:param).with("password", :string).and_return(password)
      allow(config).to receive(:param).with("uri", :string).and_return(uri)
      allow(config).to receive(:param).with("jql", :string).and_return(jql)
    end

    it "setup Embulk::Input::Jira::Api" do
      expect(Embulk::Input::Jira::Api).to receive(:setup).and_return(jira_api)
      subject
    end

    it "returns guessed config" do
      allow(Embulk::Input::Jira::Api).to receive(:setup).and_return(jira_api)

      expect(subject).to eq guessed_config
    end
  end

  describe "#init (.new)" do
    # NOTE: InputPlugin.initialize calls #init method.

    subject { Embulk::Input::JiraInputPlugin.new({}, nil, nil, nil) }

    it "setup Embulk::Input::Jira::Api" do
      expect(Embulk::Input::Jira::Api).to receive(:setup)
      subject
    end

    it "is a Embulk::InputPlugin" do
      allow(Embulk::Input::Jira::Api).to receive(:setup)
      expect(subject).to be_a(Embulk::InputPlugin)
    end
  end

  describe "#run" do
    subject do
      result = nil
      capture_output(:out) do
        result = Embulk::Input::JiraInputPlugin.new(task, nil, nil, page_builder).run
      end
      result
    end

    let(:jira_api) { Embulk::Input::Jira::Api.new }
    let(:jira_issues) do
      (1..total_count).map do |i|
        attributes = fields.merge("id" => i.to_s, "jira_key" => "FOO-#{i}")

        Embulk::Input::Jira::Issue.new(attributes)
      end
    end

    let(:total_count) { max_result + 10 }
    let(:max_result) { Embulk::Input::JiraInputPlugin::PER_PAGE }


    let(:page_builder) { Object.new } # add mock later
    let(:task) do
      {
        "jql" => jql,
        "attributes" => {"project.key" => "string"}
      }
    end

    let(:fields) do
      {
        "fields" =>
        {
          "project" => {
            "key" => project_name,
          },
        }
      }
    end

    let(:commit_report) { {} }

    before do
      allow(org.embulk.spi.Exec).to receive_message_chain(:session, :isPreview).and_return(false)

      # TODO: create stubs without each `it` expected
      allow(Embulk::Input::Jira::Api).to receive(:setup).and_return(jira_api)

      0.step(total_count, max_result) do |start_at|
        issues = jira_issues[start_at..(start_at + max_result - 1)]
        allow(jira_api).to receive(:search_issues).with(jql, start_at: start_at).and_return(issues)
      end
      allow(jira_api).to receive(:total_count).and_return(total_count)

      allow(page_builder).to receive(:add).with([project_name])
      allow(page_builder).to receive(:finish)
    end

    it 'search JIRA issues' do
      expect(jira_api).to receive(:search_issues)
      subject
    end

    it 'page build and finish' do
      expect(page_builder).to receive(:add).with([project_name]).exactly(total_count).times
      expect(page_builder).to receive(:finish)
      subject
    end

    it 'returns commit report' do
      expect(subject).to eq commit_report
    end
  end

  describe ".logger" do
    let(:logger) { Embulk::Input::JiraInputPlugin.logger }

    subject { logger }

    it { is_expected.to be_a(Logger) }
  end

  describe "#logger" do
    let(:instance) { Embulk::Input::JiraInputPlugin.new({}, nil, nil, nil) }
    let(:logger) { instance.logger }

    subject { logger }

    it { is_expected.to be_a(Logger) }
  end
end
