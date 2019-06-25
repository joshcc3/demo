package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class LastTradeDataForSymbol {

    public final String symbol;

    private LastTrade bidLastTrade;
    private LastTrade askLastTrade;

    public LastTradeDataForSymbol(final String symbol) {

        this.symbol = symbol;
    }

    public void setLastTrade(final LastTrade lastTrade) {

        if (BookSide.BID == lastTrade.getSide()) {
            bidLastTrade = lastTrade;
        } else {
            askLastTrade = lastTrade;
        }
    }

    public boolean isLastBuy(final long price) {
        return null != bidLastTrade && bidLastTrade.getPrice() == price;
    }

    public LastTrade lastBid() {
        return bidLastTrade;
    }

    public LastTrade lastAsk() {
        return askLastTrade;
    }

    public boolean isLastSell(final long price) {
        return null != askLastTrade && askLastTrade.getPrice() == price;
    }
}