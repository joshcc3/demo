MONITOR_GC=N
ZING_LOG="$LOG_DIR/zing-$SERVICE-$TODAY.$TIME.log"
GC_LOG="$LOG_DIR/gc-$SERVICE-$TODAY.$TIME.log"
JAVA_OPTS="-server -Xmx12g -Xms12g  -XX:+PrintGC -XX:+PrintGCApplicationStoppedTime -XX:+PrintGCApplicationConcurrentTime -XX:+PrintGCDateStamps -XX:+PrintHeapAtGC -Xloggc:$GC_LOG -XX:ProfileLogOut=/site/drw/reddal/reddal.profile.log -XX:ProfileLogIn=/site/drw/reddal/reddal.profile.log -XX:-DisplayVMOutput -XX:+LogVMOutput -XX:+TraceDeoptimization -XX:+PrintCompilation -XX:+PrintCompileDateStamps -XX:LogFile=$ZING_LOG"
JAVA_COMMAND="com.drwtrading.london.reddal.Main prod-equities"
JAVA=/opt/zing/zing-jdk8/bin/java