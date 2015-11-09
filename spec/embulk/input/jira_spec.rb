require "spec_helper"

describe Embulk::Input::Jira do
  let(:username) { "jira-user" }
  let(:password) { "password" }
  let(:uri) { "http://jira.example/" }
  let(:jql) { "PROJECT=#{project_name}" }
  let(:project_name) { "FOO" }

  describe ".transaction" do
    subject { described_class.transaction(config, &control) }

    let(:config) { Object.new } # add mock later
    let(:control) { Proc.new{|task, columns, count| } } # do nothing

    let(:task) do
      {
        username: username,
        password: password,
        uri: uri,
        jql: jql,
        attributes: {
          "project.key" => :string,
          "comment.total" => :long
        },
        retry_limit: 5,
        retry_initial_wait_sec: 1,
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
      allow(config).to receive(:param).with(:username, :string).and_return(username)
      allow(config).to receive(:param).with(:password, :string).and_return(password)
      allow(config).to receive(:param).with(:uri, :string).and_return(uri)
      allow(config).to receive(:param).with(:jql, :string).and_return(jql)
      allow(config).to receive(:param).with(:columns, :array).and_return(columns)
      allow(config).to receive(:param).with(:retry_limit, :integer, default: 5).and_return(5)
      allow(config).to receive(:param).with(:retry_initial_wait_sec, :integer, default: 1).and_return(1)
    end

    # NOTE: I should check other factor, but i don't know it...
    it "calls .resume method with proper parameters" do
      expect(described_class).to receive(:resume).with(task, column_structs, 1, &control)
      subject
    end
  end

  describe ".resume" do
    subject { described_class.resume(task, columns, count, &control) }

    let(:task) do
      {
        username: username,
        password: password,
        uri: uri,
        jql: jql,
        attributes: {
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
    subject { described_class.guess(config) }

    let(:config) { Object.new } # add mock later

    let(:jira_api) { Embulk::Input::JiraApi::Client.new }
    let(:jira_issues) { [Embulk::Input::JiraApi::Issue.new(attributes)] }
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
          {name: "comment.comments", type: :string},
          {name: "comment.total", type: :long},
          {name: "id", type: :long},
          {name: "key", type: :string},
          {name: "project.key", type: :string},
          {name: "project.name", type: :string},
        ]
      }
    end

    before do
      allow(jira_api).to receive(:search_issues).with(jql, max_results: described_class::GUESS_RECORDS_COUNT).and_return(jira_issues)

      allow(config).to receive(:param).with(:username, :string).and_return(username)
      allow(config).to receive(:param).with(:password, :string).and_return(password)
      allow(config).to receive(:param).with(:uri, :string).and_return(uri)
      allow(config).to receive(:param).with(:jql, :string).and_return(jql)
    end

    it "setup Embulk::Input::JiraApi::Client" do
      expect(Embulk::Input::JiraApi::Client).to receive(:setup).and_return(jira_api)
      subject
    end

    it "returns guessed config" do
      allow(Embulk::Input::JiraApi::Client).to receive(:setup).and_return(jira_api)

      expect(subject).to eq guessed_config
    end
  end

  describe "#init (.new)" do
    # NOTE: InputPlugin.initialize calls #init method.

    subject { described_class.new({}, nil, nil, nil) }

    it "setup Embulk::Input::JiraApi::Client" do
      expect(Embulk::Input::JiraApi::Client).to receive(:setup)
      subject
    end

    it "is a Embulk::InputPlugin" do
      allow(Embulk::Input::JiraApi::Client).to receive(:setup)
      expect(subject).to be_a(Embulk::InputPlugin)
    end
  end

  describe "#run" do
    subject do
      allow(plugin).to receive(:logger).and_return(::Logger.new(File::NULL))
      silence do
        plugin.run
      end
    end

    let(:plugin) { described_class.new(task, nil, nil, page_builder) }
    let(:jira_api) { Embulk::Input::JiraApi::Client.new }
    let(:jira_issues) do
      (1..total_count).map do |i|
        attributes = fields.merge("id" => i.to_s, "jira_key" => "FOO-#{i}")

        Embulk::Input::JiraApi::Issue.new(attributes)
      end
    end

    let(:total_count) { max_result + 10 }
    let(:max_result) { described_class::PER_PAGE }


    let(:page_builder) { Object.new } # add mock later
    let(:task) do
      {
        jql: jql,
        attributes: {"project.key" => "string"}
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
      allow(jira_api).to receive(:preview?).and_return(false)

      # TODO: create stubs without each `it` expected
      allow(Embulk::Input::JiraApi::Client).to receive(:setup).and_return(jira_api)

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

  describe "preview" do
    let(:plugin) { described_class.new(task, nil, nil, page_builder) }
    let(:task) do
      {
        jql: jql,
        attributes: {"project.key" => "string"}
      }
    end
    let(:page_builder) { double("page_builder") }
    let(:jira_api) { Embulk::Input::JiraApi::Client.new }
    let(:jira_issues) { [Embulk::Input::JiraApi::Issue.new(attributes)] }
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


    subject { plugin.run }

    before do
      allow(Embulk::Input::JiraApi::Client).to receive(:setup).and_return(jira_api)
      allow(plugin).to receive(:logger).and_return(::Logger.new(File::NULL))
      allow(plugin).to receive(:preview?).and_return(true)
      allow(jira_api).to receive(:search_issues).and_return(jira_issues)
      allow(page_builder).to receive(:add)
      allow(page_builder).to receive(:finish)
    end

    it "max_results with PREVIEW_RECORDS_COUNT" do
      expect(jira_api).to receive(:search_issues).with(jql, max_results: Embulk::Input::Jira::PREVIEW_RECORDS_COUNT)
      subject
    end

    it "call page_builder.add and page_builder.finish" do
      expect(page_builder).to receive(:add).exactly(jira_issues.length).times
      expect(page_builder).to receive(:finish)
      subject
    end
  end

  describe ".logger" do
    let(:logger) { described_class.logger }

    subject { logger }

    it { is_expected.to be_a(Embulk::Logger) }
  end

  describe "#logger" do
    let(:instance) { described_class.new({}, nil, nil, nil) }
    let(:logger) { instance.logger }

    subject { logger }

    it { is_expected.to be_a(Embulk::Logger) }
  end
end
