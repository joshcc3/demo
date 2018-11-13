package com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderServerRouter;

public class StopAllForMarketNumberCmd implements IOrderCmd {

    private final boolean isAcknowledged;
    private final String reason;

    public StopAllForMarketNumberCmd(final boolean isAcknowledged, final String reason) {

        this.isAcknowledged = isAcknowledged;
        this.reason = reason;
    }

    @Override
    public void route(final RemoteOrderServerRouter router) {
        router.broadcastCmd(this);
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.stopAllForMarketNumber(isAcknowledged, reason);
    }

    @Override
    public void rejectMsg(final String msg) {
        // no-op
    }
}
