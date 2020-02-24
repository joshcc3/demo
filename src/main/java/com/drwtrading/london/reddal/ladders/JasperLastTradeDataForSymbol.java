package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import drw.eeif.trades.transport.outbound.ITrade;

public class JasperLastTradeDataForSymbol {

    public final String symbol;

    private ITrade bidLastTrade;
    private ITrade askLastTrade;

    public JasperLastTradeDataForSymbol(final String symbol) {

        this.symbol = symbol;
    }


    public void setLastTrade(final ITrade lastTrade) {

        if (BookSide.BID == lastTrade.getSide()) {
            bidLastTrade = lastTrade;
        } else {
            askLastTrade = lastTrade;
        }
    }

    public ITrade lastBid() {
        return bidLastTrade;
    }

    public ITrade lastAsk() {
        return askLastTrade;
    }

}
