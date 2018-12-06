MONITOR_GC=N
ZING_LOG="$LOG_DIR/zing-$SERVICE-$TODAY.$TIME.log"
GC_LOG="$LOG_DIR/gc-$SERVICE-$TODAY.$TIME.log"
ZING_PROFILE=/site/drw/$APPNAME/$SERVICE-zing.profile
JAVA_OPTS="-server -Xmx12g -XX:+PrintGC -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -Xloggc:$GC_LOG -XX:ProfileLogOut=$ZING_PROFILE -XX:ProfileLogIn=$ZING_PROFILE -XX:-DisplayVMOutput -XX:+LogVMOutput -XX:+TraceDeoptimization -XX:+PrintCompilation -XX:+PrintCompileDateStamps -XX:LogFile=$ZING_LOG"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-equities"
JAVA=/opt/zing/zing-jdk8/bin/java

#MONITOR_GC=Y
#JAVA_OPTS="-server -verbosegc -Xmx6g -Xms6g -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=60 -XX:+PrintGCDetails"
#JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-equities"
#JAVA=/site/drw/java/jdk1.8.0_92/bin/java