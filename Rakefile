require 'rake/clean'

CLOBBER.include('dist', 'rake', 'lib')
CLEAN.include('tmp', 'build')

PROJECT="reddal"
PACKAGE_NAME = "drw.london.reddal"
TEST_JAR = "build/tmp/tests.jar"
TMP_DIR="build/tmp"
LIB="lib"
JAR = "dist/#{PACKAGE_NAME}.jar"
JAR_SRC = "dist/#{PACKAGE_NAME}-src.jar"
TARBALL="dist/#{PROJECT}-#{ENV["BUILD_NUMBER"] || "dev"}.tgz"

##### Wire in external functions

sh "fig -c rake -m"

load "rake/java.rake"
load "rake/pkg.rake"

##### Tasks

task :javalib => [:test, JAR_SRC]
task :default => :dist

desc 'Run tests'
task :test => [TEST_JAR, JAR] do |task|
	  tests = classnames('src/test/java', FileList['src/test/java/**/*Test.java'])
	  puts "Testing #{tests}"
	  classpath = os_classpath(jars(LIB) + [JAR, TEST_JAR])
	  sh "fig -c test -m -- java -classpath '#{TEST_JAR}:#{JAR}:#{classpath}' org.testng.TestNG -verbose 1 -testjar '#{TEST_JAR}'"
end

task :dist => [:clobber, :test, TARBALL] do
  puts '===================================================================='
  puts "Distributable archive  : #{TARBALL}"
  puts "To run server          : rake run"
end

task :run => [:dist] do
    sh "cd build/#{PROJECT} && ./bin/run #{PROJECT} run"
end

file JAR => FileList['src/main/java/**/*.java'] do |task|
  build_java_jar :src=>task.prerequisites, :dest=>task.name, :fig_config=>'test', :repo_info => 'none'
end

file TEST_JAR => FileList[JAR, 'src/test/java/**/*.java'] do |task|
	mkdir_p TMP_DIR
	build_java_jar :src=>task.prerequisites, :dest=>task.name, :fig_config=>'test', :classpath=>[JAR]
end

file TARBALL => FileList['bin/**/*', 'lib/**/*', 'web/**/*', 'etc/**/*', JAR] do
  build_tarball(TARBALL, PROJECT, [JAR], ['web', 'bin', 'etc'])
end
