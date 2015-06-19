# This module contains methods for Plugin.

module Embulk::Guess
  module SchemaGuess
    class << self

      # NOTE: Original #from_hash_records uses keys of the first record only,
      #       but JSON from JIRA API has fields which of value is sometimes nil,
      #       sometimes JSON,
      #       so the first record doesn't have all key for guess always.
      # original Embulk::Guess::SchemaGuess is https://github.com/embulk/embulk/blob/57b42c31d1d539177e1e818f294550cde5b69e1f/lib/embulk/guess/schema_guess.rb#L16-L24
      def from_hash_records(array_of_hash)
        array_of_hash = Array(array_of_hash)
        if array_of_hash.empty?
          raise "SchemaGuess Can't guess schema from no records"
        end
        column_names = array_of_hash.map(&:keys).inject([]) {|r, a| r + a }.uniq.sort
        samples = array_of_hash.to_a.map {|hash| column_names.map {|name| hash[name] } }
        from_array_records(column_names, samples)
      end
    end
  end
end

module Embulk
  module Input
    module JiraInputPluginUtils
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
