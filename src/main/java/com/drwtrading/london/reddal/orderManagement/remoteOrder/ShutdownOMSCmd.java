package com.drwtrading.london.reddal.orderManagement.remoteOrder;

public class ShutdownOMSCmd implements IOrderCmd {

    private final String reason;

    public ShutdownOMSCmd(final String reason) {
        this.reason = reason;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.shutdownAllOMS(reason);
    }
}
