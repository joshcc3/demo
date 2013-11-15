MONITOR_GC=Y
JAVA_OPTS="-server -verbosegc -Xmx4g -Xms4g -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+ExplicitGCInvokesConcurrent -XX:InitialCodeCacheSize=128m -XX:ReservedCodeCacheSize=128m -XX:+UseCompressedOops -XX:-DontCompileHugeMethods -XX:+DoEscapeAnalysis"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-xetra"
