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

    context "when fields_attributes is empty Hash" do
      let(:fields_attributes) { {} }

      let(:attribute_name) { 'key1' }

      it "returns nil" do
        expect(subject).to eq nil
      end
    end

    context "when fields_attributes is empty Hash including Array" do
      let(:fields_attributes) { { 'key1' => [] } }

      let(:attribute_name) { 'key1' }

      it "returns empty Array" do
        expect(subject).to eq []
      end
    end

    context "when fields_attributes is Hash" do
      let(:fields_attributes) do
        {'key' => 'value'}
      end

      let(:attribute_name) { 'key' }

      it "returns 'value'" do
        expect(subject).to eq "FOO-1"
      end
    end

    context "when fields_attributes is `{'key1' => {'key2' => 'value2', 'key3' => 'value3'}}`" do
      let(:fields_attributes) do
        {'key1' => {'key2' => 'value2', 'key3' => 'value3'}}
      end

      context "when attribute_name is 'key1'" do
        let(:attribute_name) { 'key1' }

        it "returns key1's JSON" do
          expect(subject).to eq({'key2' => 'value2', 'key3' => 'value3'}.to_json)
        end
      end

      context "when attribute_name is 'key1.key2'" do
        let(:attribute_name) { 'key1.key2' }

        it "returns 'value2'" do
          expect(subject).to eq 'value2'
        end
      end
    end

    context "when fields_attributes is `{'key1' => [{'key2' => 'value2-1'}, {'key2' => 'value2-2'}]}`" do
      let(:fields_attributes) do
        {'key1' => [{'key2' => 'value2-1'}, {'key2' => 'value2-2'}]}
      end

      context "when attribute_name is 'key1'" do
        let(:attribute_name) { 'key1' }

        it "returns JSON array" do
          expect(subject).to eq '{"key2":"value2-1"},{"key2":"value2-2"}'
        end
      end

      context "when attribute_name is 'key1.key2'" do
        let(:attribute_name) { 'key1.key2' }

        it "returns CSV values assigned by 'key2' key" do
          expect(subject).to eq 'value2-1,value2-2'
        end
      end
    end

    context "when fields_attributes is `{'key1' => [{'key2' => 'value2-1'}, nil]}`" do
      let(:fields_attributes) do
        {'key1' => [{'key2' => 'value2-1'}, nil]}
      end

      context "when attribute_name is 'key1'" do
        let(:attribute_name) { 'key1' }

        it "returns CSV value including null" do
          expect(subject).to eq '{"key2":"value2-1"},null'
        end
      end

      context "when attribute_name is 'key1.key2'" do
        let(:attribute_name) { 'key1.key2' }

        it "returns JSON array including null" do
          expect(subject).to eq 'value2-1,null'
        end
      end
    end

    context "when fields_attributes is `{'key1' => ['element1', 'element2', 'element3']}`" do
      let(:fields_attributes) do
        {
          'key1' => ['element1', 'element2', 'element3'],
        }
      end

      let(:attribute_name) { 'key1' }

      it "returns CSV values assigned by 'key1'" do
        expect(subject).to eq 'element1,element2,element3'
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

    context "when fields_attributes is `{'key' => 'value'}`" do
      let(:fields_attributes) do
        {'key' => 'value'}
      end

      let(:exptected_record_from_fields) do
        {'key' => 'value'}
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'key1' => {'key2' => 'value2', 'key3' => 'value3'}}`" do
      let(:fields_attributes) do
        {
          'key1' => {
            'key2' => 'value2',
            'key3' => 'value3',
          }
        }
      end

      let(:exptected_record_from_fields) do
        {
          "key1.key2" => "value2",
          "key1.key3" => "value3"
        }
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'key1' => {'key2' => {'key3' => last_child}}}`" do
      let(:fields_attributes) do
        {'key1' => {'key2' => {'key3' => last_child}}}
      end

      context "when last_child is String" do
        let(:last_child) do
          "String"
        end

        let(:exptected_record_from_fields) do
          {"key1.key2.key3" => "String"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child has 'key' key" do
        let(:last_child) do
          {"key" => "BAR-1"}
        end

        let(:exptected_record_from_fields) do
          {"key1.key2.key3.key" => "BAR-1"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child has 'id' key" do
        let(:last_child) do
          {"id" => "20"}
        end

        let(:exptected_record_from_fields) do
          {"key1.key2.key3.id" => "20"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child has 'name' key" do
        let(:last_child) do
          {"name" => "Foo name"}
        end

        let(:exptected_record_from_fields) do
          {"key1.key2.key3.name" => "Foo name"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child has another key except 'key', 'id', 'name'" do
        let(:last_child) do
          {"customfield_0001" => "value0001"}
        end

        let(:exptected_record_from_fields) do
          {"key1.key2.key3" => "{\"customfield_0001\":\"value0001\"}"}
        end

        it_behaves_like "return guessed record"
      end

      context "when last_child Hash Array" do
        let(:last_child) do
          [
            {"key4" => "value4", "key5" => "value5-1"},
            {"key6" => "value6", "key5" => "value5-2"},
          ]
        end

        let(:exptected_record_from_fields) do
          {"key1.key2.key3" => '{"key4":["value4",null],"key5":["value5-1","value5-2"],"key6":[null,"value6"]}'}
        end

        it_behaves_like "return guessed record"
      end
    end

    context "when fields_attributes is `{'key1' => ['element1-1', 'element1-2', 'element1-3'], 'key2' => ['element2-1', 'element2-2', 'element2-3']}`" do
      let(:fields_attributes) do
        {
          'key1' => ['element1-1', 'element1-2', 'element1-3'],
          'key2' => ['element2-1', 'element2-2', 'element2-3'],
        }
      end

      let(:exptected_record_from_fields) do
        {
          'key1' => '"element1-1,element1-2,element1-3"',
          'key2' => '"element2-1,element2-2,element2-3"',
        }
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'key' => []}`" do
      let(:fields_attributes) do
        {'key' => []}
      end

      let(:exptected_record_from_fields) do
        {"key" => '""'}
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'key1' => { 'key2' => [{'key3' => 'valu3-1'}, {'key3'=> 'value3-2'}]}}`" do
      let(:fields_attributes) do
        {
          'key1' => {
            'key2' => [
              {'key3' => 'value3-1'}, {'key3'=> 'value3-2'},
            ]
          }
        }
      end

      let(:exptected_record_from_fields) do
        {
          "key1.key2.key3" => '"value3-1,value3-2"',
        }
      end

      it_behaves_like "return guessed record"
    end

    context "when fields_attributes is `{'key1' => { 'key2' => ['element2-1', 'element2-2', 'element2-3'], 'key3' => ['element3-1', 'element3-2', 'element3-3']}}`" do
      let(:fields_attributes) do
        {
          'key1' => {
            'key2' => ['element2-1', 'element2-2', 'element2-3'],
            'key3' => ['element3-1', 'element3-2', 'element3-3']
          }
        }
      end

      let(:exptected_record_from_fields) do
        {
          "key1.key2" => '"element2-1,element2-2,element2-3"',
          "key1.key3" => '"element3-1,element3-2,element3-3"',
        }
      end

      it_behaves_like "return guessed record"
    end
  end
end
