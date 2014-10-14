package com.drwtrading.london.reddal.data;

import com.drwtrading.london.protocols.photon.marketdata.TradeUpdate;

public class TradeTracker {

    public TradeUpdate lastTrade = null;
    public int quantityTraded = 0;
    public boolean tradedOnSameLevel = false;
    public boolean lastTradedUp = false;
    public boolean lastTradedDown = false;

    public void onTradeUpdate(TradeUpdate tradeUpdate) {
        if (lastTrade != null && lastTrade.getPrice() == tradeUpdate.getPrice()) {
            quantityTraded += tradeUpdate.getQuantityTraded();
            tradedOnSameLevel = true;
        } else {
            quantityTraded = tradeUpdate.getQuantityTraded();
            tradedOnSameLevel = false;
            if (lastTrade != null) {
                lastTradedUp = tradeUpdate.getPrice() > lastTrade.getPrice();
                lastTradedDown = tradeUpdate.getPrice() < lastTrade.getPrice();
            }
        }
        lastTrade = tradeUpdate;
    }
}
