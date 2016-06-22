package com.drwtrading.london.reddal.util;

import com.drwtrading.monitoring.stats.StatsMsg;
import com.drwtrading.monitoring.stats.advisory.AdvisoryStat;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.handlers.Notifier;
import org.jetlang.channels.Publisher;

import java.util.HashSet;
import java.util.Set;

public class ConnectionCloser implements Notifier {

    private final Publisher<StatsMsg> status;
    private final String name;
    private final Set<PhotocolsConnection> closedConnections;

    public ConnectionCloser(final Publisher<StatsMsg> status, final String name) {

        this.status = status;
        this.name = name;
        this.closedConnections = new HashSet<>();
    }

    @Override
    public void onTimeout(final PhotocolsConnection connection, final Long millisSinceLastMessage) {
        if (closedConnections.add(connection)) {
            connection.close();
            status.publish(new AdvisoryStat(name, AdvisoryStat.Level.INFO, "Disconnected " + name + ", idle for " + millisSinceLastMessage + "ms"));
        }
    }

}
