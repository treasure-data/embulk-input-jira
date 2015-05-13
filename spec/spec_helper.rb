# -- coding: utf-8

require "rubygems"
require "bundler/setup"
Bundler.require(:runtime, :development)

Dir["./spec/support/**/*.rb"].each{|file| require file }

if ENV["COVERAGE"]
  require "simplecov"
  SimpleCov.start
end

$LOAD_PATH.unshift File.expand_path("../../lib", __FILE__)

RSpec.configure do |config|
end
