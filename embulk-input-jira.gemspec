Gem::Specification.new do |spec|
  spec.name          = "embulk-input-jira"
  spec.version       = "0.1.0"
  spec.authors       = ["uu59", "yoshihara"]
  spec.summary       = "Jira input plugin for Embulk"
  spec.description   = "Loads records from Jira."
  spec.email         = ["k@uu59.org", "h.yoshihara@everyleaf.com"]
  spec.licenses      = ["Apache2"]
  spec.homepage      = "https://github.com/treasure-data/embulk-input-jira"

  spec.files         = `git ls-files`.split("\n") + Dir["classpath/*.jar"]
  spec.test_files    = spec.files.grep(%r{^(test|spec)/})
  spec.require_paths = ["lib"]

  spec.add_dependency 'jiralicious', ['~> 0.5.0']
  spec.add_dependency 'parallel', ['~> 1.6.0']
  spec.add_dependency 'perfect_retry', ['~> 0.2']
  spec.add_development_dependency 'bundler', ['~> 1.0']
  spec.add_development_dependency 'rake', ['>= 10.0']
  spec.add_development_dependency 'rspec', "~> 3.2.0"
  spec.add_development_dependency 'embulk', [">= 0.6.12", "!= 0.6.22", "< 1.0"]
  spec.add_development_dependency 'simplecov'
  spec.add_development_dependency 'pry'
  spec.add_development_dependency 'codeclimate-test-reporter'
  spec.add_development_dependency 'everyleaf-embulk_helper'
end
