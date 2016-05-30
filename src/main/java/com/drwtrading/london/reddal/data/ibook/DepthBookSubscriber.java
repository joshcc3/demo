package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.reddal.data.SelectIOMDForSymbol;

public class DepthBookSubscriber implements IBookSubscriber {

    private final LevelThreeBookSubscriber l3BookSubscriber;
    private final LevelTwoBookSubscriber l2BookSubscriber;

    public DepthBookSubscriber(final LevelThreeBookSubscriber l3BookSubscriber, final LevelTwoBookSubscriber l2BookSubscriber) {

        this.l3BookSubscriber = l3BookSubscriber;
        this.l2BookSubscriber = l2BookSubscriber;
    }

    @Override
    public void subscribeForMD(final String symbol, final SelectIOMDForSymbol listener) {

        this.l3BookSubscriber.subscribeForMD(symbol, listener);
        this.l2BookSubscriber.subscribeForMD(symbol, listener);
    }

    @Override
    public void unsubscribeForMD(final String symbol) {

        this.l3BookSubscriber.unsubscribeForMD(symbol);
        this.l2BookSubscriber.unsubscribeForMD(symbol);
    }
}
