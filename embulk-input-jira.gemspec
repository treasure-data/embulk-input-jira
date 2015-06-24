Gem::Specification.new do |spec|
  spec.name          = "embulk-input-jira"
  spec.version       = "0.0.5"
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
  spec.add_development_dependency 'bundler', ['~> 1.0']
  spec.add_development_dependency 'rake', ['>= 10.0']
  spec.add_development_dependency 'rspec', "~> 3.2.0"
  spec.add_development_dependency 'simplecov'
  spec.add_development_dependency 'pry'
end
