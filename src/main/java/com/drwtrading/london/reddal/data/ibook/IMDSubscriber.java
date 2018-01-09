package com.drwtrading.london.reddal.data.ibook;

public interface IMDSubscriber {

    public MDForSymbol subscribeForMD(final String symbol, final Object listener);

    public void unsubscribeForMD(final String symbol, final Object listener);
}
