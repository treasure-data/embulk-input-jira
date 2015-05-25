# This module contains methods for Plugin.

module Embulk
  module Input
    module Utils
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
