package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.time.Clock;
import com.drwtrading.london.util.Struct;
import org.jetlang.channels.Publisher;

import java.util.Map;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class TradingStatusWatchdog {

    private final Publisher<ServerTradingStatus> tradingStatusPublisher;
    private final Map<String, Long> lastWorkingOrderEventFromServer = newFastMap();
    private final Map<String, Long> lastRemoteOrderEventFromServer = newFastMap();
    private final Map<String, ServerTradingStatus> tradingStatusMap = newFastMap();
    private final long maxAgeMillis;
    private final Clock clock;

    public TradingStatusWatchdog(final Publisher<ServerTradingStatus> tradingStatusPublisher, final long maxAgeMillis, final Clock clock) {
        this.tradingStatusPublisher = tradingStatusPublisher;
        this.maxAgeMillis = maxAgeMillis;
        this.clock = clock;
    }

    @Subscribe
    public void on(Main.WorkingOrderEventFromServer serverHeartbeat) {
        lastWorkingOrderEventFromServer.put(serverHeartbeat.fromServer, clock.now());
    }

    @Subscribe
    public void on(Main.RemoteOrderEventFromServer managementServerHeartbeat) {
        lastRemoteOrderEventFromServer.put(managementServerHeartbeat.fromServer, clock.now());
    }

    public Runnable checkRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                long now = clock.now();
                for (Map.Entry<String, Long> entry : lastWorkingOrderEventFromServer.entrySet()) {
                    Long workingOrderTime = entry.getValue();
                    Long remoteOrderTime = lastRemoteOrderEventFromServer.get(entry.getKey());
                    Status workingStatus = workingOrderTime != null && now - workingOrderTime < maxAgeMillis ? Status.OK : Status.NOT_OK;
                    Status remoteStatus = remoteOrderTime != null && now - remoteOrderTime < maxAgeMillis ? Status.OK : Status.NOT_OK;
                    Status tradingStatus = workingStatus == Status.OK && remoteStatus == Status.OK ? Status.OK : Status.NOT_OK;
                    ServerTradingStatus serverTradingStatus = new ServerTradingStatus(entry.getKey(), workingStatus, remoteStatus, tradingStatus);
                    if (!serverTradingStatus.equals(tradingStatusMap.put(serverTradingStatus.server, serverTradingStatus))) {
                        tradingStatusPublisher.publish(serverTradingStatus);
                    }
                }
            }
        };
    }

    public static enum Status {
        OK, NOT_OK
    }


    public static class ServerTradingStatus extends Struct {
        public final String server;
        public final Status workingOrderStatus;
        public final Status remoteCommandStatus;
        public final Status tradingStatus;

        public ServerTradingStatus(String server, Status workingOrderStatus, Status remoteCommandStatus, Status tradingStatus) {
            this.server = server;
            this.workingOrderStatus = workingOrderStatus;
            this.remoteCommandStatus = remoteCommandStatus;
            this.tradingStatus = tradingStatus;
        }
    }
}
