# Not to be used as a standalone guess plugin
# This is just a thin wrapper to leverage the SchemaGuess Ruby implementation from Java side

require 'json'

module Embulk
  module Guess
    class JiraGuess < TextGuessPlugin
      Plugin.register_guess("jira", self)

      def guess_text(config, sample_text)
        {:columns =>
             SchemaGuess.from_hash_records(JSON.parse(sample_text)).map do |c|
               {
                   name: c.name,
                   type: c.type,
                   **(c.format ? {format: c.format} : {})
               }
             end
        }
      end
    end
  end
end