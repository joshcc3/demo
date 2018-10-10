MONITOR_GC=Y
JAVA_OPTS="-server -verbosegc -Xmx4g -Xms4g -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=30 -XX:+PrintGCDetails"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-obligations"
JAVA=/site/drw/java/jdk1.8.0_92/bin/java
