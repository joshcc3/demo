require 'rake/clean'

sh "fig -mc rake"

CLOBBER.include('dist', 'rake', 'lib')
CLEAN.include('tmp', 'build')

PROJECT = "reddal"

##### Wire in external functions

load "rake/eeif.rake"

##### Tasks

task :dist => [:default, JAR_SRC, TARBALL]

file TARBALL => FileList['bin/**/*', 'lib/**/*', 'web/**/*', 'etc/**/*', JAR] do
  build_tarball(TARBALL, PROJECT, [JAR], ['web', 'bin', 'etc'])
end
