require "bundler/gem_tasks"
require 'rspec/core/rake_task'
require "everyleaf/embulk_helper/tasks"

Everyleaf::EmbulkHelper::Tasks.install({
  gemspec: "./embulk-input-jira.gemspec",
  github_name: "treasure-data/embulk-input-jira",
})

task default: :spec

desc "Run all examples"
RSpec::Core::RakeTask.new(:spec) do |t|
  t.rspec_opts = %w[--color]
end
