# This module contains methods for Plugin.

module Embulk
  module Input
    module Utils
      # Guess::SchemaGuess.from_hash_records returns Columns
      # containing 'index' key, but it is needless.
      def self.guess_columns(records)
        schema = Guess::SchemaGuess.from_hash_records(records)

        schema.map do |c|
          column = {name: c.name, type: c.type}
          column[:format] = c.format if c.format
          column
        end
      end

      def self.cast(value, type)
        return value if value.nil?

        case type.to_sym
        when :long
          Integer(value)
        when :double
          Float(value)
        when :timestamp
          Time.parse(value)
        when :boolean
          !!value
        else
          value.to_s
        end
      end
    end
  end
end
