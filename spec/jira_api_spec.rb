require "spec_helper"
require "jira/api"

describe Jira::Api do
  describe ".setup" do
    subject { Jira::Api.setup {}  }

    it "returns Jira::Api instance" do
      expect(subject.is_a?(Jira::Api)).to be_truthy
    end

    it "calls Jiralicious.configure" do
      allow(Jiralicious).to receive(:configure)
    end
  end

  describe "#search" do
    let(:jql) { "project=FOO" }

    subject { Jira::Api.new.search(jql) }

    it do
      allow(Jiralicious).to receive(:search).with(jql)
    end
  end
end
