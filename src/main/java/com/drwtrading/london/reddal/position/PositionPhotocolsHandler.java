package com.drwtrading.london.reddal.position;

import com.drwtrading.photocols.PhotocolsConnection;
import com.drwtrading.photocols.PhotocolsHandler;
import drw.eeif.photons.mrchill.Position;
import org.jetlang.channels.Publisher;

public class PositionPhotocolsHandler implements PhotocolsHandler<Position, Void> {

    private final Publisher<Position> positionPublisher;

    public PositionPhotocolsHandler(final Publisher<Position> positionPublisher) {
        this.positionPublisher = positionPublisher;
    }

    @Override
    public PhotocolsConnection<Void> onOpen(final PhotocolsConnection<Void> connection) {
        return connection;
    }

    @Override
    public void onConnectFailure() {

    }

    @Override
    public void onClose(final PhotocolsConnection<Void> connection) {

    }

    @Override
    public void onMessage(final PhotocolsConnection<Void> connection, final Position message) {
        positionPublisher.publish(message);
    }
}
