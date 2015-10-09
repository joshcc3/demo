package com.drwtrading.london.reddal.util;

import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import org.jetlang.core.Scheduler;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class IdleConnectionTimeoutHandler<Inbound, Outbound> implements PhotocolsHandler<Inbound, Outbound> {

    private final ConnectionCloser connectionCloser;
    private final Map<PhotocolsConnection, Long> lastTimestamp = newFastMap();
    private final long timeoutMillis;

    public IdleConnectionTimeoutHandler(ConnectionCloser connectionCloser, final long timeoutMillis, Scheduler scheduler) {
        this.connectionCloser = connectionCloser;
        this.timeoutMillis = timeoutMillis;
        long interval = Math.max(1, (timeoutMillis - 1) / 2);
        scheduler.scheduleWithFixedDelay(checkRunnable(), interval, interval, TimeUnit.MILLISECONDS);
    }

    @Override
    public PhotocolsConnection<Outbound> onOpen(PhotocolsConnection<Outbound> connection) {
        lastTimestamp.put(connection, System.currentTimeMillis());
        System.out.println("Open: " + connection.getRemoteAddress().toString());
        return connection;
    }

    @Override
    public void onConnectFailure() {
    }

    @Override
    public void onClose(PhotocolsConnection<Outbound> connection) {
        Long remove = lastTimestamp.remove(connection);
        connectionCloser.onTimeout(connection, remove == null ? timeoutMillis : System.currentTimeMillis() - remove);
    }

    @Override
    public void onMessage(PhotocolsConnection<Outbound> connection, Inbound message) {
        lastTimestamp.put(connection, System.currentTimeMillis());
    }

    public Runnable checkRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                Iterator<Map.Entry<PhotocolsConnection, Long>> iterator = lastTimestamp.entrySet().iterator();
                while (iterator.hasNext()) {
                    Map.Entry<PhotocolsConnection, Long> entry = iterator.next();
                    long millisSinceLastMessage = now - entry.getValue();
                    if (millisSinceLastMessage > timeoutMillis) {
                        final PhotocolsConnection connection = entry.getKey();
                        System.out.println(connection.getRemoteAddress() + " " + millisSinceLastMessage);
                        connectionCloser.onTimeout(connection, millisSinceLastMessage);
                        iterator.remove();
                    }
                }
            }
        };
    }
}
