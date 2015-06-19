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
        "fields" => fields_attributes
      }
    end

    context "when target attribute_name is especially key" do
      let(:fields_attributes) { {} }

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
    end

    context "when fields_attributes is `{'hoge' => 'fuga'}` and attribute_name is 'hoge'" do
      let(:fields_attributes) do
        {'hoge' => 'fuga'}
      end

      let(:attribute_name) { 'hoge' }

      it "returns 'fuga'" do
        expect(subject).to eq "fuga"
      end
    end

    context "when fields_attributes is `{'hoge' => {'fuga' => 'piyo', 'foo' => 'bar'}}`" do
      let(:fields_attributes) do
        {'hoge' => {'fuga' => 'piyo', 'foo' => 'bar'}}
      end

      context "when attribute_name is 'hoge'" do
        let(:attribute_name) { 'hoge' }

        it "returns hoge's JSON" do
          expect(subject).to eq({'fuga' => 'piyo', 'foo' => 'bar'}.to_json)
        end
      end

      context "when attribute_name is 'hoge.fuga'" do
        let(:attribute_name) { 'hoge.fuga' }

        it "returns 'piyo'" do
          expect(subject).to eq 'piyo'
        end
      end
    end

    context "when fields_attributes is `{'hoge' => [{'bar' => 'piyo1'}, {'bar' => 'piyo2'}]}`" do
      let(:fields_attributes) do
        {'hoge' => [{'bar' => 'piyo1'}, {'bar' => 'piyo2'}]}
      end

      context "when attribute_name is 'hoge'" do
        let(:attribute_name) { 'hoge' }

        it "returns JSON array" do
          expect(subject).to eq [{'bar' => 'piyo1'}, {'bar' => 'piyo2'}].to_json
        end
      end

      context "when attribute_name is 'hoge.bar'" do
        let(:attribute_name) { 'hoge.bar' }

        it "returns CSV values assigned by 'bar' key" do
          expect(subject).to eq 'piyo1,piyo2'
        end
      end
    end

    context "when fields_attributes is `{'hoge' => ['elem1', 'elem2', 'elem3']}` and attribute_name is 'hoge'" do
      let(:fields_attributes) do
        {
          'hoge' => ['elem1', 'elem2', 'elem3'],
        }
      end

      let(:attribute_name) { 'hoge' }

      it "returns CSV values assigned by 'hoge'" do
        expect(subject).to eq 'elem1,elem2,elem3'
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
