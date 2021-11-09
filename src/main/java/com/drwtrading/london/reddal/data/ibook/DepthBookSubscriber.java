package com.drwtrading.london.reddal.data.ibook;

import java.util.HashMap;
import java.util.Map;

public class DepthBookSubscriber implements IMDSubscriber {

    private final LevelThreeBookSubscriber l3BookSubscriber;
    private final LevelTwoBookSubscriber l2BookSubscriber;

    private final Map<String, MDForSymbol> mdForSymbols;

    public DepthBookSubscriber(final LevelThreeBookSubscriber l3BookSubscriber, final LevelTwoBookSubscriber l2BookSubscriber) {

        this.l3BookSubscriber = l3BookSubscriber;
        this.l2BookSubscriber = l2BookSubscriber;

        this.mdForSymbols = new HashMap<>();
    }

    @Override
    public MDForSymbol subscribeForMDCallbacks(final String symbol, final IMDCallback callback) {

        final MDForSymbol mdForSymbol = subscribeForMD(symbol, this);
        if (mdForSymbol.addMDCallback(callback)) {

            this.l3BookSubscriber.addUpdateCallback(mdForSymbol);
            this.l2BookSubscriber.addUpdateCallback(mdForSymbol);
        }

        return mdForSymbol;
    }

    @Override
    public MDForSymbol subscribeForMD(final String symbol, final Object listener) {

        final MDForSymbol mdForSymbol = mdForSymbols.get(symbol);
        if (null == mdForSymbol) {
            final MDForSymbol newMDForSymbol = new MDForSymbol(symbol, this);
            mdForSymbols.put(symbol, newMDForSymbol);
            return addListener(newMDForSymbol, listener);
        } else {
            return addListener(mdForSymbol, listener);
        }
    }

    private MDForSymbol addListener(final MDForSymbol mdForSymbol, final Object listener) {

        if (mdForSymbol.addListener(listener)) {
            this.l3BookSubscriber.subscribeForMD(mdForSymbol);
            this.l2BookSubscriber.subscribeForMD(mdForSymbol);
        }
        return mdForSymbol;
    }

    @Override
    public void unsubscribeForMD(final String symbol, final Object listener) {

        final MDForSymbol mdForSymbol = mdForSymbols.get(symbol);
        if (null != mdForSymbol && mdForSymbol.removeListener(listener)) {
            this.l3BookSubscriber.unsubscribeForMD(mdForSymbol);
            this.l2BookSubscriber.unsubscribeForMD(mdForSymbol);
        }
    }
}
