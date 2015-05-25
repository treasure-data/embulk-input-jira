require "spec_helper"
require "embulk/input/jira"

describe Embulk::Input::JiraInputPlugin do
  it do
    allow(Jira::Api).to receive(:setup)
    expect(Embulk::Input::JiraInputPlugin.new({}, nil, nil, nil)).to be_a(Embulk::InputPlugin)
  end

  describe ".init" do
    subject { Embulk::Input::JiraInputPlugin.init }

    it "setup Jira::Api" do
      allow(Jira::Api).to receive(:setup)
    end
  end

  describe "#run" do
    subject do
      Embulk::Input::JiraInputPlugin.new(task, nil, nil, page_builder).run
    end

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
            "id" => "FOO",
          },
        }
      }
    end

    let(:commit_report) { {} }

    before do
      # TODO: create stubs without each `it` expected
      allow(Jira::Api).to receive(:setup).and_return(Jira::Api.new)

      allow_any_instance_of(Jira::Api).to receive(:search_issues).and_return [Jira::Issue.new(field)]
      allow(page_builder).to receive(:add)
      allow(page_builder).to receive(:finish)
    end

    it 'search JIRA issues' do
      expect_any_instance_of(Jira::Api).to receive(:search_issues)
      subject
    end

    it 'page build and finish' do
      expect(page_builder).to receive(:add)
      expect(page_builder).to receive(:finish)
      subject
    end

    it 'returns commit report' do
      expect(subject).to eq commit_report
    end
  end

  describe ".transaction" do
    subject { Embulk::Input::JiraInputPlugin.transaction(config, &block) }
    let(:config) { Object.new } # add mock later
    let(:block) { Proc.new{|task, columns, count| } }
    let(:task) do
      {
        "username" => "hoge",
        "password" => "fuga",
        "uri" => "http://jira.example/",
        "jql" => "PROJECT=FOO",
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
      allow(config).to receive(:param).with("username", :string).and_return("hoge")
      allow(config).to receive(:param).with("password", :string).and_return("fuga")
      allow(config).to receive(:param).with("uri", :string).and_return("http://jira.example/")
      allow(config).to receive(:param).with("jql", :string).and_return("PROJECT=FOO")
      allow(config).to receive(:param).with("columns", :array).and_return(columns)
      allow(Embulk::Input::JiraInputPlugin).to receive(:resume).with(task, column_structs, 1, &block)
    end

    it "calls .resume method with proper parameters" do
      expect(Embulk::Input::JiraInputPlugin).to receive(:resume).with(task, column_structs, 1, &block)
      subject
    end
  end

  describe ".resume" do
    subject { Embulk::Input::JiraInputPlugin.resume(task, columns, count, &control) }

    let(:task) do
      {
        "username" => "hoge",
        "password" => "fuga",
        "uri" => "http://jira.example/",
        "jql" => "PROJECT=FOO",
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
end
