require 'rubygems'
require 'rake/testtask'
require 'rake/clean'
require 'rake/rdoctask'

@src_dir = FileList['src/**/*.java']
@test_dir = FileList['test/**/*.java']
@target_dir = 'target'
@lib_dir = ENV['HOME'] + '/.m2/repository/'
@classpath = ['junit/junit/4.4', 'obgenesis/objenesis/1.1']

task :default => :build_jar

task :build_jar => [:compile, :package] do
end

task :init do
  mkdir_p @target_dir
end


task :compile => [:init] do 
  cmd = 'javac  -classpath ' + @classpath.collect {|path| @lib_dir + path + ':'}.to_s + ' '  + file_list()
  puts "cmd is: " + cmd
  sh cmd
end


task :package do
end


def file_list() 
  results = ''
  list = @src_dir
  list.each { |line|
      results = results + " " + line
  }
  return results
end