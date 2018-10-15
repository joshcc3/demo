package com.drwtrading.london.reddal.safety;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportConnected;
import com.drwtrading.monitoring.stats.status.StatusStat;
import org.jetlang.channels.Publisher;

public class TradingStatusWatchdog {

    private final long maxAgeMillis;
    private final Publisher<StatusStat> statsPublisher;

    public TradingStatusWatchdog(final long maxAgeMillis, final Publisher<StatusStat> statsPublisher) {

        this.maxAgeMillis = maxAgeMillis;
        this.statsPublisher = statsPublisher;
    }

    public void setNibblerTransportConnected(final NibblerTransportConnected connectedNibbler) {

        if (connectedNibbler.isConnected) {
            statsPublisher.publish(
                    new StatusStat(connectedNibbler.nibblerName + ": trading status", StatusStat.State.GREEN, (int) maxAgeMillis));
        }
    }
}
