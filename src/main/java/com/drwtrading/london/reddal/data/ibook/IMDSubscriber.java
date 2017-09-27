package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.reddal.data.MDForSymbol;

public interface IMDSubscriber {

    public void subscribeForMD(final String symbol, final MDForSymbol listener);

    public void unsubscribeForMD(final String symbol, final MDForSymbol listener);
}
