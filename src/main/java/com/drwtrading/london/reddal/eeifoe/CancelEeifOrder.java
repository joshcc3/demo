package com.drwtrading.london.reddal.eeifoe;

import com.drwtrading.london.util.Struct;

public class CancelEeifOrder extends Struct implements EeifOrderCommand {
    public final int orderId;
    public final String serverName;

    public CancelEeifOrder(int orderId, String serverName) {
        this.orderId = orderId;
        this.serverName = serverName;
    }

    @Override
    public int getOrderId() {
        return orderId;
    }

    @Override
    public String getServer() {
        return serverName;
    }

    @Override
    public void accept(EeifOrderCommandHandler handler) {
        handler.on(this);
    }


}
