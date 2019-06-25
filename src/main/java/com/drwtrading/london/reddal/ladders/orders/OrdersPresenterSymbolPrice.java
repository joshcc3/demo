package com.drwtrading.london.reddal.ladders.orders;

import com.drwtrading.london.util.Struct;

class OrdersPresenterSymbolPrice extends Struct {

    final String symbol;
    final long price;
    final long bidPrice;
    final long askPrice;

    OrdersPresenterSymbolPrice(final String symbol, final long price, final long bidPrice, final long askPrice) {
        this.symbol = symbol;
        this.price = price;
        this.bidPrice = bidPrice;
        this.askPrice = askPrice;
    }
}
