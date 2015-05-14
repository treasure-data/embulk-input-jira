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

  describe "#search_issues" do
    let(:jql) { "project=FOO" }
    let(:results) do
      [
        {
          "fields" =>
          {
            "summary" => "issue summary",
            "project" =>
            {
              "key" => "FOO"
            }
          }
        },
        {
          "fields" =>
          {
            "summary" => "jira issue",
            "project" =>
            {
              "key" => "FOO"
            }
          }
        }
      ]
    end

    subject { Jira::Api.new.search_issues(jql) }

    it do
      allow(Jiralicious).to receive_message_chain(:search, :issues).and_return(results)

      expect(subject).to be_kind_of Array
      expect(subject.map(&:class)).to match_array [Jira::Issue, Jira::Issue]
    end
  end
end
