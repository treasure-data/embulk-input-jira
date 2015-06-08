require "spec_helper"

describe Embulk::Input::JiraInputPluginUtils do
  describe ".guess_columns" do
    subject do
      Embulk::Input::JiraInputPluginUtils.guess_columns(records)
    end

    let(:records) do
      [
        {"project_key" => "FOO", "comment.total" => 3, "created" => "2015-03-01T00:12:00"}
      ]
    end

    let(:expected) do
      [
        {name: "project_key", type: :string},
        {name: "comment.total", type: :long},
        {name: "created", type: :timestamp, format: "%Y-%m-%dT%H:%M:%S"}
      ]
    end

    it "returns Array containing columns without 'index' key from each record" do
      expect(subject).to eq expected
    end
  end

  describe ".cast" do
    subject do
      Embulk::Input::JiraInputPluginUtils.cast(value, type)
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
