package com.drwtrading.london.reddal.safety;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderEventFromServer;
import com.drwtrading.london.time.Clock;
import com.drwtrading.london.util.Struct;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.status.StatusStat;
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
    private final Publisher<StatsMsg> statsPublisher;

    public TradingStatusWatchdog(final Publisher<ServerTradingStatus> tradingStatusPublisher, final long maxAgeMillis, final Clock clock,
            final Publisher<StatsMsg> statsPublisher) {
        this.tradingStatusPublisher = tradingStatusPublisher;
        this.maxAgeMillis = maxAgeMillis;
        this.clock = clock;
        this.statsPublisher = statsPublisher;
    }

    @Subscribe
    public void on(final WorkingOrderEventFromServer serverHeartbeat) {
        lastWorkingOrderEventFromServer.put(serverHeartbeat.fromServer, clock.now());
    }

    @Subscribe
    public void on(final Main.RemoteOrderEventFromServer managementServerHeartbeat) {
        lastRemoteOrderEventFromServer.put(managementServerHeartbeat.fromServer, clock.now());
    }

    public void checkHeartbeats() {
        final long now = clock.now();
        for (final Map.Entry<String, Long> entry : lastWorkingOrderEventFromServer.entrySet()) {
            final Long workingOrderTime = entry.getValue();
            final Long remoteOrderTime = lastRemoteOrderEventFromServer.get(entry.getKey());
            final Status workingStatus = workingOrderTime != null && now - workingOrderTime < maxAgeMillis ? Status.OK : Status.NOT_OK;
            final Status remoteStatus = remoteOrderTime != null && now - remoteOrderTime < maxAgeMillis ? Status.OK : Status.NOT_OK;
            final Status tradingStatus = workingStatus == Status.OK && remoteStatus == Status.OK ? Status.OK : Status.NOT_OK;
            final ServerTradingStatus serverTradingStatus =
                    new ServerTradingStatus(entry.getKey(), workingStatus, remoteStatus, tradingStatus);
            if (!serverTradingStatus.equals(tradingStatusMap.put(serverTradingStatus.server, serverTradingStatus))) {
                tradingStatusPublisher.publish(serverTradingStatus);
            }
            if (workingStatus == Status.OK) {
                statsPublisher.publish(
                        new StatusStat(entry.getKey() + ": working order status", StatusStat.State.GREEN, (int) maxAgeMillis));
            }
            if (remoteStatus == Status.OK) {
                statsPublisher.publish(
                        new StatusStat(entry.getKey() + ": remote order status", StatusStat.State.GREEN, (int) maxAgeMillis));
            }
            if (tradingStatus == Status.OK) {
                statsPublisher.publish(new StatusStat(entry.getKey() + ": trading status", StatusStat.State.GREEN, (int) maxAgeMillis));
            }
        }
    }

    public static enum Status {
        OK,
        NOT_OK
    }

    public static class ServerTradingStatus extends Struct {

        public final String server;
        public final Status workingOrderStatus;
        public final Status remoteCommandStatus;
        public final Status tradingStatus;

        public ServerTradingStatus(final String server, final Status workingOrderStatus, final Status remoteCommandStatus,
                final Status tradingStatus) {
            this.server = server;
            this.workingOrderStatus = workingOrderStatus;
            this.remoteCommandStatus = remoteCommandStatus;
            this.tradingStatus = tradingStatus;
        }
    }
}
