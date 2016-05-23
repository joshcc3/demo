package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.reddal.data.SelectIOMDForSymbol;

public interface IBookHandler {

    public void subscribeForMD(final String symbol, final SelectIOMDForSymbol listener);

    public void unsubscribeForMD(final String symbol);
}
