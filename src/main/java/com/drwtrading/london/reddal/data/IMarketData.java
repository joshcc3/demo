package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.reddal.util.PriceOperations;

public interface IMarketData {

    public void subscribeForMD();

    public void unsubscribeForMD();

    public PriceOperations getPriceOperations();

    public boolean isPriceInverted();

    public IBook<?> getBook();

    public TradeTracker getTradeTracker();

    public String formatPrice(final long price);
}
