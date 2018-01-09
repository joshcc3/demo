package com.drwtrading.london.reddal.data.ibook;

public class NoMDSubscriptions implements IMDSubscriber {

    @Override
    public MDForSymbol subscribeForMD(final String symbol, final Object listener) {
        return new MDForSymbol(symbol);
    }

    @Override
    public void unsubscribeForMD(final String symbol, final Object listener) {
        // no-op
    }
}
