package com.drwtrading.london.reddal.util;

import com.drwtrading.london.time.Clock;
import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.status.StatusStat;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import org.jetlang.channels.Publisher;

public class PhotocolsStatsPublisher<Inbound, Outbound> implements PhotocolsHandler<Inbound, Outbound> {

    private final Publisher<StatsMsg> statsPublisher;
    private final String statsName;
    private final int secondsValid;
    private final DoOnceEveryXMillis doOnceEveryXMillis;

    public PhotocolsStatsPublisher(Publisher<StatsMsg> statsPublisher, String statsName, int secondsValid) {
        this.statsPublisher = statsPublisher;
        this.statsName = statsName;
        this.secondsValid = secondsValid;
        doOnceEveryXMillis = new DoOnceEveryXMillis(Clock.SYSTEM, secondsValid * 1000 / 3);
    }

    @Override
    public PhotocolsConnection<Outbound> onOpen(PhotocolsConnection<Outbound> connection) {
        return connection;
    }

    @Override
    public void onConnectFailure() {
    }

    @Override
    public void onClose(PhotocolsConnection<Outbound> connection) {
    }

    @Override
    public void onMessage(PhotocolsConnection<Outbound> connection, Inbound message) {
        doOnceEveryXMillis.doItEveryXMillis(new Runnable() {
            @Override
            public void run() {
                statsPublisher.publish(new StatusStat(statsName, StatusStat.State.GREEN, secondsValid));
            }
        });
    }
}
