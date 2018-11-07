package com.drwtrading.london.reddal.orderManagement.remoteOrder;

public class StopAllForMarketNumberCmd implements IOrderCmd {

    private final boolean isAcknowledged;
    private final String reason;

    public StopAllForMarketNumberCmd(final boolean isAcknowledged, final String reason) {

        this.isAcknowledged = isAcknowledged;
        this.reason = reason;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.stopAllForMarketNumber(isAcknowledged, reason);
    }
}
