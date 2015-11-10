package com.drwtrading.london.reddal.eeifoe;

import com.drwtrading.london.util.Struct;

public class SubmitEeifOrder extends Struct implements EeifOrderCommand {
    public final EeifOrder order;

    public SubmitEeifOrder(EeifOrder order) {
        this.order = order;
    }

    @Override
    public int getOrderId() {
        return order.orderId;
    }

    @Override
    public String getServer() {
        return order.serverName;
    }

    @Override
    public void accept(EeifOrderCommandHandler handler) {
        handler.on(this);
    }
}
