package com.drwtrading.london.reddal.workingOrders.opxl;

public class BestWorkingPriceForSymbol {

    final String symbol;

    final Long bidPrice;
    final Long bidQty;

    final Long askPrice;
    final Long askQty;

    public BestWorkingPriceForSymbol(final String symbol, final Long bidPrice, final Long bidQty, final Long askPrice, final Long askQty) {

        this.symbol = symbol;

        this.bidPrice = bidPrice;
        this.bidQty = bidQty;

        this.askPrice = askPrice;
        this.askQty = askQty;
    }
}
