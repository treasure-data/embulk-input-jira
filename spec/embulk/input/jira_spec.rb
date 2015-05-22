require "embulk/command/embulk_run"
classpath_dir = Embulk.home("classpath")
jars = Dir.entries(classpath_dir).select {|f| f =~ /\.jar$/ }.sort
jars.each do |jar|
  require File.join(classpath_dir, jar)
end
require "embulk/java/bootstrap"
require "embulk"
require "embulk/input/jira"

require "spec_helper"

describe Embulk::Input::JiraInputPlugin do
  describe "#cast" do
    subject do
      allow(Jira::Api).to receive(:setup)
      Embulk::Input::JiraInputPlugin.new({}, nil, nil, nil).send(:cast, value, type)
    end

    context "when value is nil" do
      let(:value) { nil }
      let(:type) { :string }

      it "returns nil" do
        expect(subject).to be_nil
      end
    end

    context "when value is not nil" do
      let(:value) { 123 }

      context "and type is :string" do
        let(:type) { :string }

        it "returns '123'" do
          expect(subject).to eq "123"
        end
      end

      context "and type is :long" do
        let(:type) { :long }

        it "returns 123" do
          expect(subject).to eq 123
        end
      end

      context "and type is :double" do
        let(:type) { :double }

        it "returns 123.0" do
          expect(subject).to eq 123.0
        end
      end

      context "and type is :timestamp" do
        let(:value) { "2015-03-01T00:12:00" }
        let(:type) { :timestamp }

        it "returns Time object" do
          expect(subject).to eq Time.new(2015, 3, 1, 0, 12, 0)
        end
      end

      context "and type is :boolean" do
        let(:type) { :boolean }

        it "returns true" do
          expect(subject).to be true
        end
      end
    end
  end
end
