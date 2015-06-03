# TODO: move to spec/jira/api_spec.rb

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
    let(:api) { Jira::Api.new }

    subject { api.search(jql) }

    it do
      allow(Jiralicious).to receive(:search).with(jql)
    end

    describe "retry and timeout" do
      before do
        allow(Timeout).to receive(:timeout) { raise Timeout::Error }
        allow(api).to receive(:sleep)
      end

      it "retry SEARCH_RETRY_TIMES times then raise error" do
        expect(Timeout).to receive(:timeout).exactly(Jira::Api::SEARCH_RETRY_TIMES)
        expect { subject }.to raise_error
      end
    end
  end

  describe "#search_issues" do
    let(:jql) { "project=FOO" }
    let(:results) do
      [
        {
          "id" => 1,
          "jira_key" => "FOO-1",
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
          "id" => 2,
          "jira_key" => "FOO-2",
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

  describe "#total_count" do
    subject { jira_api.total_count(jql) }

    let(:jira_api) { Jira::Api.new }
    let(:jql) { "project=FOO" }
    let(:results) { Object.new } # add mock later
    let(:results_count) { 5 }

    before do
      allow(results).to receive(:num_results).and_return(results_count)
    end

    it "calls Jira::Api#search with proper arguments" do
      expect(jira_api).to receive(:search).with(jql, max_results: 1).and_return(results)
      subject
    end

    it "returns issues count" do
      allow(jira_api).to receive(:search).with(jql, max_results: 1).and_return(results)
      expect(subject).to eq results_count
    end
  end
end
