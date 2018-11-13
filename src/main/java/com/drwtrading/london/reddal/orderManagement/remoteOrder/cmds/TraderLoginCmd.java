package com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds;

import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderServerRouter;

import java.util.Set;

public class TraderLoginCmd implements IOrderCmd {

    private final String toServer;

    private final Set<User> users;

    public TraderLoginCmd(final String toServer, final Set<User> users) {

        this.toServer = toServer;
        this.users = users;
    }

    @Override
    public void route(final RemoteOrderServerRouter router) {
        router.routeCmd(toServer, this);
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.traderLogin(users);
    }

    @Override
    public void rejectMsg(final String msg) {
        // no-op
    }
}
