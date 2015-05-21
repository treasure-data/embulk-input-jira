module Jira
  class Issue
    SUPPORTED_ATTRIBUTES = ["summary", "project"].map(&:freeze).freeze
    SUPPORTED_ATTRIBUTE_NAMES = SUPPORTED_ATTRIBUTES.map {|attr| "'#{attr}'"}.join(', ')

    attr_reader :fields

    def self.detect_attribute_type(attribute_name)
      case attribute_name
      when "summary", "project"
        :string
      else
        raise "Unsupported attribute_name: #{attribute_name}."
      end
    end

    def initialize(attributes)
      @fields = attributes.fetch("fields")
    end

    def [](attribute)
      fields = @fields
      attribute.split('.').each do |chunk|
        fields = fields.fetch(chunk)
      end

      if fields.is_a?(Array) || fields.is_a?(Hash)
        fields.to_json.to_s
      else
        fields
      end
    end
  end
end
