package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.utils.application.User;

import java.util.Set;

public class TraderLoginCmd implements IOrderCmd {

    private final Set<User> users;

    public TraderLoginCmd(final Set<User> users) {
        this.users = users;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.traderLogin(users);
    }
}
