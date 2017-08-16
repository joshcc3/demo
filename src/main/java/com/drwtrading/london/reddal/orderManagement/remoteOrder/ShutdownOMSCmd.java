package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import eeif.execution.RemoteOrderManagementCommand;
import eeif.execution.RemoteShutdownOms;

public class ShutdownOMSCmd implements IOrderCmd {

    private final String reason;

    private final String serverName;
    private final String username;

    public ShutdownOMSCmd(final String reason, final String serverName, final String username) {
        this.reason = reason;
        this.serverName = serverName;
        this.username = username;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.shutdownAllOMS(reason);
    }

    @Override
    public RemoteOrderManagementCommand createRemoteOrderManager(final String serverName) {
        return new RemoteShutdownOms(serverName, username, reason);
    }
}
