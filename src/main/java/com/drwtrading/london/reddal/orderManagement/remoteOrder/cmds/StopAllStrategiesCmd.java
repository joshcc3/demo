package com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderServerRouter;

public class StopAllStrategiesCmd implements IOrderCmd {

    private final String toServer;

    private final String reason;

    public StopAllStrategiesCmd(final String toServer, final String reason) {

        this.toServer = toServer;
        this.reason = reason;
    }

    @Override
    public void route(final RemoteOrderServerRouter router) {
        router.routeCmd(toServer, this);
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.stopAllStrategies(reason);
    }

    @Override
    public void rejectMsg(final String msg) {
        // no-op
    }
}
