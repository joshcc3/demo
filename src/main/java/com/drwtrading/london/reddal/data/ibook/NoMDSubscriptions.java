package com.drwtrading.london.reddal.data.ibook;

public class NoMDSubscriptions implements IMDSubscriber {

    public NoMDSubscriptions() {
    }

    @Override
    public MDForSymbol subscribeForMD(final String symbol, final Object listener) {
        return new MDForSymbol(symbol, null);
    }

    @Override
    public MDForSymbol subscribeForMDCallbacks(final String symbol, final IMDCallback callback) {
        return new MDForSymbol(symbol, null);
    }

    @Override
    public void unsubscribeForMD(final String symbol, final Object listener) {
        // no-op
    }
}
