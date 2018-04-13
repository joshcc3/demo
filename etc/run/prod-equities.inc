MONITOR_GC=Y
JAVA_OPTS="-server -verbosegc -Xmx6g -Xms6g -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=60 -XX:+PrintGCDetails"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-equities"
JAVA=/site/drw/java/jdk1.8.0_92/bin/java
