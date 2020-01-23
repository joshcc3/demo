MONITOR_GC=N
ZING_DATE=$(date '+%Y-%m-%d')
ZING_LOG="$LOG_DIR/zing-$SERVICE-$ZING_DATE.$TIME.log"
GC_LOG="$LOG_DIR/gc-$SERVICE-$TODAY.$TIME.log"
ZING_PROFILE=/site/drw/$APPNAME/$SERVICE-zing.profile
JAVA_OPTS="-server -Xmx12g -XX:+PrintGC -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -Xloggc:$GC_LOG -XX:ProfileLogOut=$ZING_PROFILE -XX:ProfileLogIn=$ZING_PROFILE -XX:-DisplayVMOutput -XX:+LogVMOutput -XX:+TraceDeoptimization -XX:+PrintCompilation -XX:+PrintCompileDateStamps -XX:LogFile=$ZING_LOG"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-equities"
JAVA=/site/drw/java/zing-jdk11.0.0-19.12.100.0-1/bin/java