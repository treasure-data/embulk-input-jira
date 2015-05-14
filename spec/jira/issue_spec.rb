require "spec_helper"
require "jira/issue"

describe Jira::Issue do
  describe ".detect_attribute_type" do
    subject { Jira::Issue.detect_attribute_type(attribute_name) }

    context 'summary' do
      let(:attribute_name) { 'summary' }

      it "returns :string" do
        expect(subject).to eq :string
      end
    end

    context 'project' do
      let(:attribute_name) { 'project' }

      it "returns :string" do
        expect(subject).to eq :string
      end
    end
  end

  describe ".initialize" do
    context "when argument has 'fields' key" do
      let(:issue_attributes) do
        {"fields" =>
          {
            "summary" => "jira issue",
            "project" =>
            {
              "key" => "FOO",
            },
          }
        }
      end

      it "has @field with argument['fields']" do
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
      {"fields" =>
        {
          "summary" => "jira issue",
          "project" =>
          {
            "key" => "FOO",
          },
        }
      }
    end

    context 'summary' do
      let(:attribute_name) { 'summary' }

      it "returns issue summary" do
        expect(subject).to eq 'jira issue'
      end
    end

    context 'project' do
      let(:attribute_name) { 'project' }

      it "returns issue's project key" do
        expect(subject).to eq 'FOO'
      end
    end
  end
end
