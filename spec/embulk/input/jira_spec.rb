require "spec_helper"
require "embulk/input/jira"

describe Embulk::Input::JiraInputPlugin do
  let(:username) { "jira-user" }
  let(:password) { "password" }
  let(:uri) { "http://jira.example/" }
  let(:jql) { "PROJECT=#{project_name}" }
  let(:project_name) { "FOO" }

  describe ".transaction" do
    subject { Embulk::Input::JiraInputPlugin.transaction(config, &control) }

    let(:config) { Object.new } # add mock later
    let(:control) { Proc.new{|task, columns, count| } }
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

    let(:jira_api) { Jira::Api.new }
    let(:jira_issues) { [Jira::Issue.new(field)] }
    let(:field) do
      {
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
        "username" => username,
        "password" => password,
        "uri" => uri,
        "api_version" => "latest",
        "auth_type" => "basic",
        "columns" => [
          {name: "project.name", type: :string},
          {name: "comment", type: :string}
        ]
      }
    end

    before do
      allow(jira_api).to receive(:search_issues).and_return(jira_issues)

      allow(config).to receive(:param).with("username", :string).and_return(username)
      allow(config).to receive(:param).with("password", :string).and_return(password)
      allow(config).to receive(:param).with("uri", :string).and_return(uri)
      allow(config).to receive(:param).with("jql", :string).and_return(jql)
    end

    it "setup Jira::Api" do
      expect(Jira::Api).to receive(:setup).and_return(jira_api)
      subject
    end

    it "returns guessed config" do
      allow(Jira::Api).to receive(:setup).and_return(jira_api)

      expect(subject).to eq guessed_config
    end
  end

  describe "#init (.new)" do
    # NOTE: InputPlugin.initialize calls #init method.

    subject { Embulk::Input::JiraInputPlugin.new({}, nil, nil, nil) }

    it "setup Jira::Api" do
      expect(Jira::Api).to receive(:setup)
      subject
    end

    it "is a Embulk::InputPlugin" do
      allow(Jira::Api).to receive(:setup)
      expect(subject).to be_a(Embulk::InputPlugin)
    end
  end

  describe "#run" do
    subject do
      Embulk::Input::JiraInputPlugin.new(task, nil, nil, page_builder).run
    end

    let(:jira_api) { Jira::Api.new }
    let(:jira_issues) { [Jira::Issue.new(field)] }

    let(:page_builder) { Object.new } # add mock later
    let(:task) do
      {
        "attributes" => {"project.key" => "string"}
      }
    end

    let(:field) do
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
      # TODO: create stubs without each `it` expected
      allow(Jira::Api).to receive(:setup).and_return(jira_api)
      allow(jira_api).to receive(:search_issues).and_return(jira_issues)
      allow(page_builder).to receive(:add).with([project_name])
      allow(page_builder).to receive(:finish)
    end

    it 'search JIRA issues' do
      expect(jira_api).to receive(:search_issues)
      subject
    end

    it 'page build and finish' do
      expect(page_builder).to receive(:add).with([project_name])
      expect(page_builder).to receive(:finish)
      subject
    end

    it 'returns commit report' do
      expect(subject).to eq commit_report
    end
  end
end
