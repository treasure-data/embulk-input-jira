require "spec_helper"

describe Embulk::Input::Jira::Issue do
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
        expect(Embulk::Input::Jira::Issue.new(issue_attributes).id).to eq issue_attributes["id"]
      end

      it "has @key with argument['jira_key']" do
        expect(Embulk::Input::Jira::Issue.new(issue_attributes).key).to eq issue_attributes["jira_key"]
      end

      it "has @fields with argument['fields']" do
        expect(Embulk::Input::Jira::Issue.new(issue_attributes).fields).to eq issue_attributes["fields"]
      end
    end

    context "when argument doesn't have 'fields' key" do
      let(:issue_attributes) do
        {}
      end

      it "raises error" do
        expect { Embulk::Input::Jira::Issue.new(issue_attributes) }.to raise_error
      end
    end
  end

  describe "#[]" do
    subject { Embulk::Input::Jira::Issue.new(issue_attributes)[attribute_name] }

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
      Embulk::Input::Jira::Issue.new(issue_attributes).to_record
    end

    shared_examples 'return guessed record' do
      it do
        expect(subject).to eq expected_record
      end
    end

    let(:issue_attributes) do
      {"jira_key" => "FOO-1", "id" => "1", "fields" => fields_attributes}
    end

    let(:expected_record) do
      {
        "key" => "FOO-1",
        "id" => "1"
      }.merge(exptected_record_from_fields)
    end

    context "when fields_attributes is `{'hoge' => 'fuga'}`" do
      let(:fields_attributes) do
        {'hoge' => 'fuga'}
      end

      let(:exptected_record_from_fields) do
        {'hoge' => 'fuga'}
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'hoge' => {'fuga' => 'piyo', 'foo' => 'bar'}}`" do
      let(:fields_attributes) do
        {'hoge' => {'fuga' => 'piyo', 'foo' => 'bar'}}
      end

      let(:exptected_record_from_fields) do
        {
          "hoge.fuga" => "piyo",
          "hoge.foo" => "bar"
        }
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'hoge' => {'fuga' => {'piyo' => last_child}}}`" do
      let(:fields_attributes) do
        {'hoge' => {'fuga' => {'piyo' => last_child}}}
      end

      context "when last_child is String" do
        let(:last_child) do
          "String"
        end

        let(:exptected_record_from_fields) do
          {"hoge.fuga.piyo" => "String"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child has 'key' key" do
        let(:last_child) do
          {"key" => "BAR-1"}
        end

        let(:exptected_record_from_fields) do
          {"hoge.fuga.piyo.key" => "BAR-1"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child has 'id' key" do
        let(:last_child) do
          {"id" => "20"}
        end

        let(:exptected_record_from_fields) do
          {"hoge.fuga.piyo.id" => "20"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child has 'name' key" do
        let(:last_child) do
          {"name" => "Foo name"}
        end

        let(:exptected_record_from_fields) do
          {"hoge.fuga.piyo.name" => "Foo name"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child has another key except 'key', 'id', 'name'" do
        let(:last_child) do
          {"bar" => "piyo"}
        end

        let(:exptected_record_from_fields) do
          {"hoge.fuga.piyo" => "{\"bar\":\"piyo\"}"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child Hash Array" do
        let(:last_child) do
          [
            {"key4" => "value1", "key5" => "value2"},
            {"key6" => "value3", "key5" => "value4"},
          ]
        end

        let(:exptected_record_from_fields) do
          {"hoge.fuga.piyo" => '{"key4":["value1",null],"key5":["value2","value4"],"key6":[null,"value3"]}'}
        end

        it_behaves_like "return guessed record"
      end
    end

    context "when fields_attributes is `{'hoge' => ['elem1', 'elem2', 'elem3'], 'fuga' => ['elem4', 'elem5', 'elem6']}`" do
      let(:fields_attributes) do
        {
          'hoge' => ['elem1', 'elem2', 'elem3'],
          'fuga' => ['elem4', 'elem5', 'elem6'],
        }
      end

      let(:exptected_record_from_fields) do
        {
          "hoge" => '"elem1,elem2,elem3"',
          "fuga" => '"elem4,elem5,elem6"',
        }
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'hoge' => []}`" do
      let(:fields_attributes) do
        {'hoge' => []}
      end

      let(:exptected_record_from_fields) do
        {"hoge" => '""'}
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'hoge' => { 'fuga' => [{'foo' => 'bar1'}, {'foo'=> 'bar2'}]}}`" do
      let(:fields_attributes) do
        {
          'hoge' => {
            'fuga' => [
              {'foo' => 'bar1'}, {'foo'=> 'bar2'},
            ]
          }
        }
      end

      let(:exptected_record_from_fields) do
        {
          "hoge.fuga.foo" => '"bar1,bar2"',
        }
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'hoge' => { 'fuga' => ['elem1', 'elem2', 'elem3'], 'piyo' => ['elem4', 'elem5', 'elem6']}}`" do
      let(:fields_attributes) do
        {'hoge' => {'fuga' => ['elem1', 'elem2', 'elem3'], 'piyo' => ['elem4', 'elem5', 'elem6']}}
      end

      let(:exptected_record_from_fields) do
        {
          "hoge.fuga" => '"elem1,elem2,elem3"',
          "hoge.piyo" => '"elem4,elem5,elem6"',
        }
      end

      it_behaves_like "return guessed record"
    end
  end
end
