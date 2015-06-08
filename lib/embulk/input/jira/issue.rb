module Embulk
  module Input
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
            return id
          when "key"
            return key
          end

          chunk = fields
          attribute.split('.').each do |key|
            chunk = chunk[key]
            return chunk if chunk.nil?
          end

          if chunk.is_a?(Array) || chunk.is_a?(Hash)
            chunk.to_json.to_s
          else
            chunk
          end
        end

        def to_record
          record = {}

          record["id"] = id
          record["key"] = key

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
  end
end
