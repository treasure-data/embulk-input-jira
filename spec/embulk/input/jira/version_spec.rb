require "spec_helper"

describe Embulk::Input::Jira::VERSION do
  subject { Embulk::Input::Jira::VERSION }

  it "x.y.z format" do
    expect(subject).to match(/\A[0-9]+\.[0-9]+\.[0-9]+\z/)
  end
end
