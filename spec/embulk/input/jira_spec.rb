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
end
