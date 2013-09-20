require 'rake/clean'

CLOBBER.include('dist', 'rake', 'lib')
CLEAN.include('tmp', 'build')

PROJECT="reddal"

JAR = "dist/#{PROJECT}.jar"
TESTS_JAR="build/#{PROJECT}-tests.jar"
TEST_DIR='src/test/java'
TARBALL="dist/#{PROJECT}-#{ENV["BUILD_NUMBER"] || "dev"}.tgz"

##### Wire in external functions

sh "fig -c rake -m"

load "rake/java.rake"
load "rake/pkg.rake"

##### Tasks

task :default => :dist

task :dist => [:test, TARBALL] do
  puts '===================================================================='
  puts "Distributable archive  : #{TARBALL}"
  puts "To run server          : rake run"
end

task :run => [:dist] do
    sh "cd build/#{PROJECT} && ./bin/run #{PROJECT} run"
end

task :test => [JAR, TESTS_JAR] do |task|
  tests = classnames(TEST_DIR, FileList[TEST_DIR+'/**/*Test.java'])
  puts
  puts "Testing #{tests}"
  puts
  junit :tests => tests, :jar=>JAR, :fig_config=>'test', :classpath=>[TESTS_JAR, JAR]
end

file JAR => FileList['src/main/java/**/*.java'] do |task|
  build_java_jar :src=>task.prerequisites, :dest=>task.name, :fig_config=>'build'
end

file TESTS_JAR => FileList[TEST_DIR  + '/**/*.java', JAR] do |task|
  build_java_jar :src=>task.prerequisites, :dest=>task.name, :classpath => [JAR], :fig_config=>'test'
end

file TARBALL => FileList['bin/**/*', 'lib/**/*', 'web/**/*', 'etc/**/*', JAR] do
  build_tarball(TARBALL, PROJECT, [JAR], ['web', 'bin', 'etc'])
end
