MONITOR_GC=Y
JAVA_OPTS="-server -verbosegc -Xmx8g -Xms8g -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=60 -XX:+PrintGCDetails -XX:G1NewSizePercent=20"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-equities"
JAVA=/site/drw/java/jdk1.8.0_92/bin/java
