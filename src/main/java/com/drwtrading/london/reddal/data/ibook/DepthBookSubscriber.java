package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.reddal.data.MDForSymbol;

public class DepthBookSubscriber {

    private final LevelThreeBookSubscriber l3BookSubscriber;
    private final LevelTwoBookSubscriber l2BookSubscriber;

    public DepthBookSubscriber(final LevelThreeBookSubscriber l3BookSubscriber, final LevelTwoBookSubscriber l2BookSubscriber) {

        this.l3BookSubscriber = l3BookSubscriber;
        this.l2BookSubscriber = l2BookSubscriber;
    }

    public void subscribeForMD(final String symbol, final MDForSymbol listener) {

        this.l3BookSubscriber.subscribeForMD(symbol, listener);
        this.l2BookSubscriber.subscribeForMD(symbol, listener);
    }

    public void unsubscribeForMD(final String symbol) {

        this.l3BookSubscriber.unsubscribeForMD(symbol);
        this.l2BookSubscriber.unsubscribeForMD(symbol);
    }
}
