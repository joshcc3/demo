package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.util.Struct;

public class CommandTrace extends Struct {

    public final String command;
    public final User user;
    public final String symbol;
    public final String orderType;
    public final boolean autoHedge;
    public final long price;
    public final String side;
    public final String tag;
    public final int quantity;
    public final int chainId;

    public CommandTrace(final String command, final User user, final String symbol, final String orderType, final boolean autoHedge,
            final long price, final String side, final String tag, final int quantity, final int chainId) {

        this.command = command;
        this.user = user;
        this.symbol = symbol;
        this.orderType = orderType;
        this.autoHedge = autoHedge;
        this.price = price;
        this.side = side;
        this.tag = tag;
        this.quantity = quantity;
        this.chainId = chainId;
    }
}
