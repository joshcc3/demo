MONITOR_GC=Y
JAVA_OPTS="-server -verbosegc -Xmx8g -Xms8g -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled -XX:+ExplicitGCInvokesConcurrent -XX:InitialCodeCacheSize=128m -XX:ReservedCodeCacheSize=128m -XX:+UseCompressedOops -XX:-DontCompileHugeMethods -XX:+DoEscapeAnalysis"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-futures"
PATH=/site/drw/java/jdk1.7.0_60/bin:$PATH