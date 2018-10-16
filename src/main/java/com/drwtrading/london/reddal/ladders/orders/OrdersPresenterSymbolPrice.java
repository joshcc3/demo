package com.drwtrading.london.reddal.ladders.orders;

import com.drwtrading.london.util.Struct;

class OrdersPresenterSymbolPrice extends Struct {

    final String symbol;
    final long price;

    OrdersPresenterSymbolPrice(final String symbol, final long price) {
        this.symbol = symbol;
        this.price = price;
    }
}
