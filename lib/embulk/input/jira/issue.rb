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

          attribute_keys = attribute.split('.')

          fetch(fields, attribute_keys)
        end

        def fetch(fields, keys)
          return fields if fields.nil?

          if keys.empty?
            return fields if !fields.is_a?(Hash) && !fields.is_a?(Array)

            if fields.is_a?(Hash) || fields.any? { |element| element.is_a?(Hash) }
              return fields.to_json
            else
              return fields.map(&:to_s).join(",")
            end
          end

          key = keys.shift
          if fields.is_a?(Array)
            fetch(fields.map {|hash| hash[key] }, keys)
          else
            fetch(fields[key], keys)
          end
        end

        def to_record
          @record = {}

          @record["id"] = id
          @record["key"] = key

          generate_record(fields, "")

          @record
        end

        private

        def generate_record(value, prefix_key)
          case value
          when Hash
            # NOTE: If you want to flatten JSON completely, please
            # remove this if...end and #add_heuristic_value.
            if prefix_key.count(".") > 1
              add_heuristic_value(value, prefix_key)
              return
            end

            value.each_pair do |_key, _value|
              generate_record(_value, record_key(prefix_key, _key))
            end
          when Array
            if value.empty? || value.any? {|v| !v.is_a?(Hash) }
              @record[prefix_key] = "\"#{value.map(&:to_s).join(',')}\""
              return
            end

            # gathering values from each Hash
            keys = value.map(&:keys).inject([]) {|sum, key| sum + key }.uniq
            values = value.inject({}) do |sum, elem|
              keys.each {|key| sum[key] = (sum[key] || []) << elem[key] }
              sum
            end

            generate_record(values, prefix_key)
          else
            @record[prefix_key] = value
          end
        end

        def record_key(prefix, key)
          return key if prefix.empty?

          "#{prefix}.#{key}"
        end

        def add_heuristic_value(hash, prefix_key)
          heuristic_values = hash.select do |key, value|
            ["name", "key", "id"].include?(key) && !value.nil?
          end

          if heuristic_values.empty?
            @record[prefix_key] = hash.to_json
          else
            heuristic_values.each do |key, value|
              @record[record_key(prefix_key, key)] = value
            end
          end
        end
      end
    end
  end
end
