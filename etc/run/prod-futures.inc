MONITOR_GC=N
ZING_DATE=$(date '+%Y-%m-%d')
ZING_LOG="$LOG_DIR/zing-$SERVICE-$ZING_DATE.$TIME.log"
GC_LOG="$LOG_DIR/gc-$SERVICE-$TODAY.$TIME.log"
ZING_PROFILE=/site/drw/$APPNAME/$SERVICE-zing.profile
JAVA_OPTS="-server -Xmx8g -XX:+PrintGC -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -Xloggc:$GC_LOG -XX:ProfileLogOut=$ZING_PROFILE -XX:ProfileLogIn=$ZING_PROFILE -XX:-DisplayVMOutput -XX:+LogVMOutput -XX:+TraceDeoptimization -XX:+PrintCompilation -XX:+PrintCompileDateStamps -XX:LogFile=$ZING_LOG"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-futures"
JAVA=/opt/zing/zing-jdk8/bin/java

#MONITOR_GC=Y
#JAVA_OPTS="-server -verbosegc -Xmx4g -Xms4g -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=30 -XX:+PrintGCDetails"
#JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-futures"
#JAVA=/site/drw/java/jdk1.8.0_92/bin/java

