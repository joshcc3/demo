package com.drwtrading.london.reddal.safety;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportConnected;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderEventFromServer;
import com.drwtrading.london.time.Clock;
import com.drwtrading.monitoring.stats.status.StatusStat;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TradingStatusWatchdog {

    private final Publisher<ServerTradingStatus> tradingStatusPublisher;

    private final long maxAgeMillis;
    private final Clock clock;
    private final Publisher<StatusStat> statsPublisher;

    private final Map<String, Long> lastWorkingOrderEventFromServer;
    private final Set<String> connectedNibblerTransports;

    private final Map<String, ServerTradingStatus> tradingStatusMap;

    public TradingStatusWatchdog(final Publisher<ServerTradingStatus> tradingStatusPublisher, final long maxAgeMillis, final Clock clock,
            final Publisher<StatusStat> statsPublisher) {

        this.tradingStatusPublisher = tradingStatusPublisher;
        this.maxAgeMillis = maxAgeMillis;
        this.clock = clock;
        this.statsPublisher = statsPublisher;

        this.lastWorkingOrderEventFromServer = new HashMap<>();
        this.connectedNibblerTransports = new HashSet<>();

        this.tradingStatusMap = new HashMap<>();
    }

    public void setWorkingOrderHeartbeat(final WorkingOrderEventFromServer serverHeartbeat) {
        lastWorkingOrderEventFromServer.put(serverHeartbeat.fromServer, clock.now());
    }

    public void setNibblerTransportConnected(final NibblerTransportConnected connectedNibbler) {

        if (connectedNibbler.isConnected) {
            connectedNibblerTransports.add(connectedNibbler.nibblerName);
        } else {
            connectedNibblerTransports.remove(connectedNibbler.nibblerName);
        }
    }

    public void checkHeartbeats() {

        final long now = clock.now();

        for (final Map.Entry<String, Long> entry : lastWorkingOrderEventFromServer.entrySet()) {

            final Long workingOrderTime = entry.getValue();
            final boolean workingStatus = null != workingOrderTime && now - workingOrderTime < maxAgeMillis;
            final boolean remoteStatus = connectedNibblerTransports.contains(entry.getKey());
            final boolean tradingStatus = workingStatus && remoteStatus;

            final ServerTradingStatus serverTradingStatus = new ServerTradingStatus(entry.getKey(), workingStatus, tradingStatus);

            if (!serverTradingStatus.equals(tradingStatusMap.put(serverTradingStatus.server, serverTradingStatus))) {
                tradingStatusPublisher.publish(serverTradingStatus);
            }
            if (workingStatus) {
                statsPublisher.publish(
                        new StatusStat(entry.getKey() + ": working order status", StatusStat.State.GREEN, (int) maxAgeMillis));
            }
            if (remoteStatus) {
                statsPublisher.publish(
                        new StatusStat(entry.getKey() + ": remote order status", StatusStat.State.GREEN, (int) maxAgeMillis));
            }
            if (tradingStatus) {
                statsPublisher.publish(new StatusStat(entry.getKey() + ": trading status", StatusStat.State.GREEN, (int) maxAgeMillis));
            }
        }
    }
}
