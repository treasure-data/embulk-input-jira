module Jira
  class Issue
    attr_reader :fields

    def initialize(attributes)
      @fields = attributes.fetch("fields")
    end

    def [](attribute)
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

    def generate_record
      record = {}
      fields.each_pair do |key, value|
        field_key = key.dup

        if value.is_a?(String)
          field_value = value
        else

          # TODO: refactor...
          if value.is_a?(Hash)
            if value.keys.include?("name")
              field_key << ".name"
              field_value = value["name"]
            elsif value.keys.include?("id")
              field_key << ".id"
              field_value = value["id"]
            else
              field_value = value.to_json.to_s
            end
          else
            field_value = value.to_json.to_s
          end
        end

        record[field_key] = field_value
      end

      record
    end
  end
end
