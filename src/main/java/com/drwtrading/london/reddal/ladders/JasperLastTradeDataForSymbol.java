package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.trades.MrChillTrade;

public class JasperLastTradeDataForSymbol {

    public final String symbol;

    private MrChillTrade bidLastTrade;
    private MrChillTrade askLastTrade;

    JasperLastTradeDataForSymbol(final String symbol) {

        this.symbol = symbol;
    }

    public void setLastTrade(final MrChillTrade lastTrade) {

        if (BookSide.BID == lastTrade.side) {
            bidLastTrade = lastTrade;
        } else {
            askLastTrade = lastTrade;
        }
    }

    public MrChillTrade lastBid() {
        return bidLastTrade;
    }

    MrChillTrade lastAsk() {
        return askLastTrade;
    }

}
