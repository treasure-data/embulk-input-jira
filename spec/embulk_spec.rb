require "spec_helper"

describe Embulk do
  describe "registerd jira as input plugin" do
    let(:config) { <<-YAML }
in:
  type: jira
    YAML
    let(:config_file) { Tempfile.new("embulk-conf") }

    before do
      config_file.puts config
      config_file.close
      allow(org.embulk.spi.Exec).to receive(:isPreview).and_return(true)
    end

    subject {
      capture(:out) do
        Embulk.run ["preview", config_file.path]
      end
    }

    it do
      # NOTE: can't stub `exit` so stubbing `raise`
      # https://github.com/embulk/embulk/blob/v0.6.9/lib/embulk/command/embulk_run.rb#L335
      allow(Embulk).to receive(:raise)

      is_expected.to_not include("InputPlugin 'jira' is not found.")
    end
  end
end
