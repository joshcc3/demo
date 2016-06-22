package com.drwtrading.london.reddal.util;

import com.drwtrading.london.time.Clock;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.status.StatusStat;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import org.jetlang.channels.Publisher;

public class PhotocolsStatsPublisher<I, O> implements PhotocolsHandler<I, O> {

    private final Publisher<StatsMsg> statsPublisher;
    private final String statsName;
    private final int secondsValid;
    private final DoOnceEveryXMillis doOnceEveryXMillis;

    public PhotocolsStatsPublisher(final Publisher<StatsMsg> statsPublisher, final String statsName, final int secondsValid) {
        this.statsPublisher = statsPublisher;
        this.statsName = statsName;
        this.secondsValid = secondsValid;
        doOnceEveryXMillis = new DoOnceEveryXMillis(Clock.SYSTEM, secondsValid * 1000 / 3);
    }

    @Override
    public PhotocolsConnection<O> onOpen(final PhotocolsConnection<O> connection) {
        return connection;
    }

    @Override
    public void onConnectFailure() {
    }

    @Override
    public void onClose(final PhotocolsConnection<O> connection) {
    }

    @Override
    public void onMessage(final PhotocolsConnection<O> connection, final I message) {
        doOnceEveryXMillis.doItEveryXMillis(() -> statsPublisher.publish(new StatusStat(statsName, StatusStat.State.GREEN, secondsValid)));
    }
}
