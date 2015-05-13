require "spec_helper"
require "embulk/input/jira_api"

describe JiraApi do
  describe ".setup" do
    subject { JiraApi.setup {}  }

    it "returns JiraApi instance" do
      expect(subject.is_a?(JiraApi)).to be_truthy
    end

    it "calls Jiralicious.configure" do
      allow(Jiralicious).to receive(:configure)
    end
  end

  describe "#search" do
    let(:jql) { "project=FOO" }

    subject { JiraApi.new.search(jql) }

    it do
      allow(Jiralicious).to receive(:search).with(jql)
    end
  end
end
