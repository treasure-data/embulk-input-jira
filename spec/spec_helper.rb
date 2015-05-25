require "rubygems"
require "bundler/setup"
Bundler.require(:runtime, :development)
require "codeclimate-test-reporter"
CodeClimate::TestReporter.start

require "embulk/command/embulk_run"

Dir["./spec/support/**/*.rb"].each{|file| require file }

if ENV["COVERAGE"]
  require "simplecov"
  SimpleCov.start
end

$LOAD_PATH.unshift File.expand_path("../../lib", __FILE__)

RSpec.configure do |config|
  config.include StdoutAndErrCapture
  config.include PrepareEmbulk
end
