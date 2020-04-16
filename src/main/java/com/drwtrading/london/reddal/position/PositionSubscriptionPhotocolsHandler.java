package com.drwtrading.london.reddal.position;

import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.photons.mrphil.PositionSubscription;
import com.drwtrading.photons.mrphil.Subscription;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.jetlang.channels.Publisher;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class PositionSubscriptionPhotocolsHandler implements PhotocolsHandler<Position, Subscription> {

    private final Set<String> allSymbols = new HashSet<>();
    private PhotocolsConnection<Subscription> connection = null;
    private final Publisher<Position> positionPublisher;

    public PositionSubscriptionPhotocolsHandler(final Publisher<Position> positionPublisher) {
        this.positionPublisher = positionPublisher;
    }

    public void setSearchResult(final SearchResult searchResult) {
        allSymbols.add(searchResult.symbol);
        subscribe(searchResult.symbol);
    }

    private void subscribe(final String symbol) {
        if (connection != null) {
            connection.send(new PositionSubscription(UUID.randomUUID().toString(), symbol, new ObjectArraySet<>()));
        }
    }

    @Override
    public PhotocolsConnection<Subscription> onOpen(final PhotocolsConnection<Subscription> connection) {
        this.connection = connection;
        for (final String symbol : allSymbols) {
            subscribe(symbol);
        }
        return connection;
    }

    @Override
    public void onConnectFailure() {
        connection = null;
    }

    @Override
    public void onClose(final PhotocolsConnection<Subscription> connection) {
        this.connection = null;
    }

    @Override
    public void onMessage(final PhotocolsConnection<Subscription> connection, final Position message) {
        this.positionPublisher.publish(message);
    }
}
