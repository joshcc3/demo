package com.drwtrading.london.reddal.util;

import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.handlers.Notifier;
import org.jetlang.channels.Publisher;

import java.util.Set;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastSet;

public class ConnectionCloser implements Notifier {

    private final Publisher<StatsMsg> status;
    private final String name;
    private Runnable runnable;
    Set<PhotocolsConnection> closedConnections = newFastSet();

    public ConnectionCloser(Publisher<StatsMsg> status, String name) {
        this.status = status;
        this.name = name;
    }

    public ConnectionCloser(Publisher<StatsMsg> status, String name, Runnable runnable) {
        this.status = status;
        this.name = name;
        this.runnable = runnable;
    }

    @Override
    public void onTimeout(PhotocolsConnection connection, Long millisSinceLastMessage) {
        if (closedConnections.add(connection)) {
            connection.close();
            status.publish(new AdvisoryStat(name, AdvisoryStat.Level.INFO, "Disconnected " + name + ", idle for " + millisSinceLastMessage + "ms"));
            if (runnable != null) {
                runnable.run();
            }
        }
    }

}
