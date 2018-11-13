package com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderServerRouter;

public interface IOrderCmd {

    public void route(final RemoteOrderServerRouter router);

    public void execute(final NibblerTransportOrderEntry client);

    public void rejectMsg(final String msg);
}
