package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.reddal.data.MDForSymbol;

public class NoMDSubscriptions implements IMDSubscriber {

    @Override
    public void subscribeForMD(final String symbol, final MDForSymbol listener) {
        // no-op
    }

    @Override
    public void unsubscribeForMD(final String symbol, final MDForSymbol listener) {

    }
}
