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
  end
end
