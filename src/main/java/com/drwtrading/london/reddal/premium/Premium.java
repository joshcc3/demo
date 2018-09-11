package com.drwtrading.london.reddal.premium;

public class Premium {

    final String symbol;
    final double midMarketPremium;
    final double lastTradPremium;


    Premium(final String symbol, final Double midMarketPremium, final double lastTradPremium) {

        this.symbol = symbol;
        this.midMarketPremium = midMarketPremium;
        this.lastTradPremium = lastTradPremium;
    }
}
