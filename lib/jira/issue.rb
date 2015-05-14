module Jira
  class Issue
    SUPPORTED_ATTRIBUTES = ["summary", "project"].map(&:freeze).freeze
    SUPPORTED_ATTRIBUTE_NAMES = SUPPORTED_ATTRIBUTES.map {|attr| "'#{attr}'"}.join(', ')

    attr_reader :fields

    def initialize(raw)
      @raw = raw
      @fields = @raw.fetch("fields")
    end

    def [](attribute)
      case attribute
      when "summary"
      @fields["summary"]
      when "project"
        @fields["project"]["key"]
      end
    end
  end
end
