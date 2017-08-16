package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import eeif.execution.RemoteOrderManagementCommand;

public interface IOrderCmd {

    public void execute(final NibblerTransportOrderEntry client);

    public RemoteOrderManagementCommand createRemoteOrderManager(final String serverName);
}
