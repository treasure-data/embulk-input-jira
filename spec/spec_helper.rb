require "rubygems"
require "bundler/setup"

Bundler.require(:runtime, :development)

require "embulk"
Embulk.setup

Dir["./spec/support/**/*.rb"].each{|file| require file }

if ENV["COVERAGE"]
  if ENV["CI"]
    require "codeclimate-test-reporter"
    CodeClimate::TestReporter.start
  else
    require 'simplecov'
    SimpleCov.start 'test_frameworks'
  end
end

$LOAD_PATH.unshift File.expand_path("../../lib", __FILE__)
require "embulk/input/jira"

RSpec.configure do |config|
  config.include StdoutAndErrCapture
end
