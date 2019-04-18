package com.drwtrading.london.reddal.auctionTrader;

public class SubmitAuctionBasket {

    final String trader;
    final int numOfBaskets;
    final int bps;

    public SubmitAuctionBasket(final String trader, final int numOfBaskets, final int bps) {
        this.trader = trader;
        this.numOfBaskets = numOfBaskets;
        this.bps = bps;
    }
}
