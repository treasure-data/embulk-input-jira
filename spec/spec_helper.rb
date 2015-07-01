require "rubygems"
require "bundler/setup"

require File.expand_path("../support/prepare_embulk.rb", __FILE__)
# Bundler.require should call after above line
Bundler.require(:runtime, :development)

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
  config.include PrepareEmbulk
end
