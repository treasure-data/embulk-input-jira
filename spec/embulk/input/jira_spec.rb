require "spec_helper"
require "embulk/input/jira"

describe Embulk::Input::JiraInputPlugin do
  it do
    allow(Jira::Api).to receive(:setup)
    expect(Embulk::Input::JiraInputPlugin.new({}, nil, nil, nil)).to be_a(Embulk::InputPlugin)
  end

  # TODO: add specs for methods inherited from Embulk::InputPlugin
end
