package com.drwtrading.london.reddal.position;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import com.drwtrading.photons.mrphil.Position;
import com.drwtrading.photons.mrphil.PositionSubscription;
import com.drwtrading.photons.mrphil.Subscription;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;
import org.jetlang.channels.Publisher;

import java.util.Set;
import java.util.UUID;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastSet;

public class PositionSubscriptionPhotocolsHandler implements PhotocolsHandler<Position, Subscription> {
    Set<String> allSymbols = newFastSet();
    PhotocolsConnection<Subscription> connection = null;
    final Publisher<Position> positionPublisher;

    public PositionSubscriptionPhotocolsHandler(Publisher<Position> positionPublisher) {
        this.positionPublisher = positionPublisher;
    }

    @Subscribe
    public void on(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        String symbol = instrumentDefinitionEvent.getSymbol();
        allSymbols.add(symbol);
        subscribe(symbol);
    }

    private void subscribe(String symbol) {
        if (connection != null) {
            connection.send(new PositionSubscription(UUID.randomUUID().toString(), symbol, new ObjectArraySet<String>()));
        }
    }

    @Override
    public PhotocolsConnection<Subscription> onOpen(PhotocolsConnection<Subscription> connection) {
        this.connection = connection;
        for (String symbol : allSymbols) {
            subscribe(symbol);
        }
        return connection;
    }

    @Override
    public void onConnectFailure() {
        connection = null;
    }

    @Override
    public void onClose(PhotocolsConnection<Subscription> connection) {
        this.connection = null;
    }

    @Override
    public void onMessage(PhotocolsConnection<Subscription> connection, Position message) {
       this.positionPublisher.publish(message);
    }
}