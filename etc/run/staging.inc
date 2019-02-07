MONITOR_GC=Y
ZING_LOG=${GC_LOG:-"$LOG_DIR/zing-$SERVICE-$TODAY.$TIME.log"}
JAVA_OPTS="-server -verbosegc -Xmx12g -Xms12g -XX:ProfileLogOut=/site/drw/reddal/reddal.profile.log -XX:ProfileLogIn=/site/drw/reddal/reddal.profile.log -XX:-DisplayVMOutput -XX:+LogVMOutput -XX:+TraceDeoptimization -XX:+PrintCompilation -XX:+PrintCompileDateStamps -XX:LogFile=$ZING_LOG"
JAVA_COMMAND="com.drwtrading.london.reddal.Main staging"
JAVA=/opt/zing/zing-jdk8/bin/java
