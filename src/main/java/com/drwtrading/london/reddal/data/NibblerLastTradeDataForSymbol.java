package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import drw.eeif.trades.transport.outbound.ITrade;

import javax.imageio.ImageTranscoder;

public class NibblerLastTradeDataForSymbol {

    public final String symbol;

    private LastTrade bidLastTrade;
    private LastTrade askLastTrade;
    private LastTrade jasperBidLastTrade;
    private LastTrade jasperAskLastTrade;

    public NibblerLastTradeDataForSymbol(final String symbol) {

        this.symbol = symbol;
    }

    public void setLastTrade(final LastTrade lastTrade) {

        if (BookSide.BID == lastTrade.getSide()) {
            bidLastTrade = lastTrade;
        } else {
            askLastTrade = lastTrade;
        }
    }

    public LastTrade lastBid() {
        return bidLastTrade;
    }

    public LastTrade lastAsk() {
        return askLastTrade;
    }

}