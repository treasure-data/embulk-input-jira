require "spec_helper"
require "jira/issue"

describe Jira::Issue do
  describe ".initialize" do
    context "when argument has 'fields' key" do
      let(:issue_attributes) do
        {
          "id" => 1,
          "jira_key" => "PRO-1",
          "fields" =>
          {
            "summary" => "jira issue",
            "project" =>
            {
              "key" => "FOO",
            },
          }
        }
      end

      it "has @id with argument['id']" do
        expect(Jira::Issue.new(issue_attributes).id).to eq issue_attributes["id"]
      end

      it "has @key with argument['jira_key']" do
        expect(Jira::Issue.new(issue_attributes).key).to eq issue_attributes["jira_key"]
      end

      it "has @fields with argument['fields']" do
        expect(Jira::Issue.new(issue_attributes).fields).to eq issue_attributes["fields"]
      end
    end

    context "when argument doesn't have 'fields' key" do
      let(:issue_attributes) do
        {}
      end

      it "raises error" do
        expect { Jira::Issue.new(issue_attributes) }.to raise_error
      end
    end
  end

  describe "#[]" do
    subject { Jira::Issue.new(issue_attributes)[attribute_name] }

    let(:issue_attributes) do
      {
        "id" => "1",
        "jira_key" => "FOO-1",
        "fields" => {
          "summary" => "jira issue",
          "project" => project_attribute,
          "labels" =>
          [
            "Feature",
            "WantTo"
          ],
          "priority" => {
            "iconUrl" => "https://jira-api/images/icon.png",
            "name" => "Must",
            "id" => "1"
          },
          "customfield_1" => nil,
        }
      }
    end

    let(:project_attribute) do
      {
        "key" => "FOO",
      }
    end

    context 'id' do
      let(:attribute_name) { 'id' }

      it "returns issue id" do
        expect(subject).to eq "1"
      end
    end

    context 'key' do
      let(:attribute_name) { 'key' }

      it "returns issue key" do
        expect(subject).to eq "FOO-1"
      end
    end

    context 'summary' do
      let(:attribute_name) { 'summary' }

      it "returns issue summary" do
        expect(subject).to eq 'jira issue'
      end
    end

    context 'project.key' do
      let(:attribute_name) { 'project.key' }

      context "when project is not nil" do
        it "returns issue's project key" do
          expect(subject).to eq 'FOO'
        end
      end

      context "when project is nil" do
        let(:project_attribute) { nil }

        it "returns nil" do
          expect(subject).to be_nil
        end
      end
    end

    context 'labels' do
      let(:attribute_name) { 'labels' }

      it "returns issue's labels JSON string" do
        expect(subject).to eq '["Feature","WantTo"]'
      end
    end

    context 'priority' do
      let(:attribute_name) { 'priority' }

      it "returns issue's priority JSON string" do
        expect(subject).to eq '{"iconUrl":"https://jira-api/images/icon.png","name":"Must","id":"1"}'
      end
    end
  end

  describe "#to_record" do
    subject do
      Jira::Issue.new(issue_attributes).to_record
    end

    let(:issue_attributes) do
      {
        "id" => 1,
        "jira_key" => "FOO-1",
        "fields" => {
          "summary" => "jira issue",
          "project" => {
            "id" => "FOO",
          },
          "labels" =>
          [
            "Feature",
            "WantTo"
          ],
          "priority" => {
            "iconUrl" => "https://jira-api/images/icon.png",
            "name" => "Must",
            "id" => "1"
          },
          "customfield_1" => nil,
        }
      }
    end

    let(:expected) do
      {
        "id" => 1,
        "key" => "FOO-1",
        "summary" => "jira issue",
        "project.id" => "FOO",
        "labels" => "[\"Feature\",\"WantTo\"]",
        "priority.name" => "Must",
        "customfield_1" => "null"
      }
    end

    it 'return guessed record' do
      expect(subject).to eq expected
    end
  end
end
