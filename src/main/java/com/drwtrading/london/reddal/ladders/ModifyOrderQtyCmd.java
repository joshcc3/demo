package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.util.Struct;

public class ModifyOrderQtyCmd extends Struct implements ISingleOrderCommand {

    public final String symbol;

    private final String orderKey;
    public final String username;
    public final int newRemainingQuantity;

    ModifyOrderQtyCmd(final String symbol, final String orderKey, final String username, final int newRemainingQuantity) {
        this.symbol = symbol;
        this.orderKey = orderKey;
        this.username = username;
        this.newRemainingQuantity = newRemainingQuantity;
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
