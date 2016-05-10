MONITOR_GC=Y
JAVA_OPTS="-server -verbosegc -Xmx2g -Xms2g -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+ExplicitGCInvokesConcurrent -XX:InitialCodeCacheSize=128m -XX:ReservedCodeCacheSize=128m -XX:+UseCompressedOops -XX:-DontCompileHugeMethods -XX:+DoEscapeAnalysis"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-gairloch"
JAVA=/site/drw/java/jdk1.8.0_45/bin/java
