MONITOR_GC=Y
JAVA_OPTS="-server -verbosegc -Xmx8g -Xms8g -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+ExplicitGCInvokesConcurrent -XX:InitialCodeCacheSize=128m -XX:ReservedCodeCacheSize=128m -XX:+UseCompressedOops -XX:-DontCompileHugeMethods -XX:+DoEscapeAnalysis"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-equities"
PATH=/home/prodappadmin/java-7-oracle/bin:$PATH
