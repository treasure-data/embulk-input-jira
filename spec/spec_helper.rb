require "rubygems"
require "bundler/setup"

Bundler.require(:runtime, :development)

require "embulk"
Embulk.setup

Dir["./spec/support/**/*.rb"].each{|file| require file }

require "codeclimate-test-reporter"
CodeClimate::TestReporter.start

if ENV["COVERAGE"]
  require "simplecov"
  SimpleCov.start
end

$LOAD_PATH.unshift File.expand_path("../../lib", __FILE__)
require "embulk/input/jira"

RSpec.configure do |config|
  config.include StdoutAndErrCapture
end
