package com.drwtrading.london.reddal.orderManagement.remoteOrder;

public class StopAllStrategiesCmd implements IOrderCmd {

    private final String reason;

    public StopAllStrategiesCmd(final String reason) {
        this.reason = reason;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.stopAllStrategies(reason);
    }
}
