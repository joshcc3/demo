package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.md.transport.tcpShaped.io.MDTransportClient;

public class LevelThreeBookSubscriber {

    public final LevelThreeBookHandler bookHandler;
    public final MDTransportClient mdClient;

    public LevelThreeBookSubscriber(final LevelThreeBookHandler bookHandler, final MDTransportClient mdClient) {

        this.bookHandler = bookHandler;
        this.mdClient = mdClient;
    }

}
