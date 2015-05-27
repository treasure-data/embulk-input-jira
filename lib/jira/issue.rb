module Jira
  class Issue
    attr_reader :id, :key, :fields

    def initialize(attributes)
      @id = attributes.fetch("id")

      # https://github.com/dorack/jiralicious/blob/404b7b6d5b7020f42064cf8d7a745ab02057e728/lib/jiralicious/issue.rb#L11-L12
      @key = attributes.fetch("jira_key")
      @fields = attributes.fetch("fields")
    end

    def [](attribute)
      case attribute
      when "id"
        return @id
      when "key"
        return @key
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

      record["key"] = @key
      record["id"] = @id

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
