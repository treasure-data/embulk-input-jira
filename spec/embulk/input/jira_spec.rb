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
end
