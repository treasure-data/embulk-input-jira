require "spec_helper"

describe Embulk::Input::JiraApi::Client do
  describe ".setup" do
    subject { Embulk::Input::JiraApi::Client.setup {}  }

    it "returns Embulk::Input::JiraApi::Client instance" do
      expect(subject.is_a?(Embulk::Input::JiraApi::Client)).to be_truthy
    end

    it "calls Jiralicious.configure" do
      allow(Jiralicious).to receive(:configure)
    end
  end

  describe "#search" do
    let(:jql) { "project=FOO" }
    let(:api) { Embulk::Input::JiraApi::Client.new }

    subject { api.search(jql) }

    it do
      allow(Jiralicious).to receive(:search).with(jql)
    end

    describe "retry and timeout" do
      before do
        allow(Timeout).to receive(:timeout) { raise Timeout::Error }
        allow(api).to receive(:sleep)
      end

      it "retry DEFAULT_SEARCH_RETRY_TIMES times then raise error" do
        expect(Timeout).to receive(:timeout).exactly(Embulk::Input::JiraApi::Client::DEFAULT_SEARCH_RETRY_TIMES + 1)
        expect { subject }.to raise_error
      end
    end
  end

  describe "#search_issues" do
    let(:jql) { "project=FOO" }
    let(:jira_api) { Embulk::Input::JiraApi::Client.new }
    let(:title_401) { "Unauthorized (401)"}
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
              "key" => "FO1"
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
              "key" => "FO2"
            }
          }
        }
      ]
    end

    subject { jira_api.search_issues(jql) }

    it "Search issues successfully" do
      allow(Jiralicious).to receive_message_chain(:search, :issues_raw).and_return(results)
      allow(Jiralicious::Issue).to receive(:find).and_return(results.first)

      expect(subject).to be_kind_of Array
      expect(subject.map(&:class)).to match_array [Embulk::Input::JiraApi::Issue, Embulk::Input::JiraApi::Issue]
    end

    it "Search issues got 401 due to high concurrent load issues" do
      allow(Jiralicious).to receive_message_chain(:search, :issues_raw).and_return(results)
      allow(jira_api).to receive(:find_issue){ MultiJson.load("<title>#{title_401}</title>")}
      allow(jira_api).to receive(:sleep)

      expect { subject }.to raise_error(StandardError, title_401)
    end
  end

  describe "#total_count" do
    subject { jira_api.total_count(jql) }

    let(:jira_api) { Embulk::Input::JiraApi::Client.new }
    let(:jql) { "project=FOO" }
    let(:results) { Object.new } # add mock later
    let(:results_count) { 5 }

    before do
      allow(results).to receive(:num_results).and_return(results_count)
    end

    it "calls Embulk::Input::JiraApi::Client#search with proper arguments" do
      expect(jira_api).to receive(:search).with(jql, max_results: 1).and_return(results)
      subject
    end

    it "returns issues count" do
      allow(jira_api).to receive(:search).with(jql, max_results: 1).and_return(results)
      expect(subject).to eq results_count
    end
  end

  describe "#timeout_and_retry" do
    let(:wait) { 1 }
    let(:retry_times) { 3 }
    let(:jira_api) { Embulk::Input::JiraApi::Client.new }
    let(:block) { proc{ "it works" } }

    subject { jira_api.send(:timeout_and_retry, wait, retry_times, &block) }

    before do
      allow(jira_api).to receive(:sleep)
    end

    it "return given block result if timeout is not occured" do
      expect(subject).to eq block.call
    end

    it "Always timeout, raise error after N times retry" do
      allow(Timeout).to receive(:timeout) { raise Timeout::Error }

      expect(Timeout).to receive(:timeout).with(wait).exactly(retry_times + 1).times
      expect { subject }.to raise_error(Timeout::Error)
    end

    describe "invalid JSON response" do
      let(:block) { proc{ MultiJson.load("<title>#{title}</title>")} }
      before { allow(Embulk.logger).to receive(:warn) }

      # Disable this test to enable retry for 401
      # context "Unauthorized" do
      #   let(:title) { "Unauthorized (401)" }
      #
      #   it do
      #     expect { subject }.to raise_error(Embulk::ConfigError)
      #   end
      # end

      context "Unavailable" do
        let(:title) { "Atlassian Cloud Notifications - Page Unavailable"}

        it do
          expect { subject }.to raise_error(StandardError, title)
        end
      end
    end
  end
end
