module Jira
  class Issue
    JIRA_ID = "id"
    ISSUE_ID = "id"

    attr_reader :id, :fields

    def initialize(attributes)
      @id = attributes.fetch(JIRA_ID)
      @fields = attributes.fetch("fields")
    end

    def [](attribute)
      case attribute
      when ISSUE_ID
        return @id
      end

      fields = @fields
      attribute.split('.').each do |chunk|
        fields = fields[chunk]
        return fields if fields.nil?
      end

      if fields.is_a?(Array) || fields.is_a?(Hash)
        fields.to_json.to_s
      else
        fields
      end
    end

    def to_record
      record = {}

      record[ISSUE_ID] = @id

      fields.each_pair do |key, value|
        record_key = key
        record_value = value.to_json.to_s

        case value
        when String
          record_value = value
        when Hash
          if value.keys.include?("name")
            record_key += ".name"
            record_value = value["name"]
          elsif value.keys.include?("id")
            record_key += ".id"
            record_value = value["id"]
          end
        end

        record[record_key] = record_value
      end

      record
    end
  end
end
