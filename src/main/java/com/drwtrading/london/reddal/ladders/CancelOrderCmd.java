package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.util.Struct;

public class CancelOrderCmd extends Struct implements ISingleOrderCommand {

    public final String symbol;
    private final String orderKey;
    public final String username;

    CancelOrderCmd(final String symbol, final String orderKey, final String username) {
        this.symbol = symbol;
        this.orderKey = orderKey;
        this.username = username;
    }

    @Override
    public String getSymbol() {
        return symbol;
    }

    @Override
    public String getOrderKey() {
        return orderKey;
    }

    @Override
    public String getUsername() {
        return username;
    }
}
