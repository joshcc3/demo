package com.drwtrading.london.reddal.eeifoe;

import com.drwtrading.london.photons.eeifoe.Order;
import com.drwtrading.london.protocols.photon.execution.Side;
import com.drwtrading.london.util.Struct;

public class EeifOrder extends Struct {

    public final int orderId;
    public final String symbol;
    public final Side side;
    public final long price;
    public final int qty;
    public final String user;
    public final String tag;
    public final OrderEntryClient.EeifOrderType orderType;
    public final String serverName;
    public final Order order;

    public EeifOrder(int orderId, String symbol, Side side, long price, int qty, String user, String tag, OrderEntryClient.EeifOrderType orderType, String serverName) {
        this.orderId = orderId;
        this.symbol = symbol;
        this.side = side;
        this.price = price;
        this.qty = qty;
        this.user = user;
        this.tag = tag;
        this.orderType = orderType;
        this.serverName = serverName;
        order = this.orderType.getOrder(this);
    }

}
