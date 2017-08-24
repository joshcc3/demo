package com.drwtrading.london.reddal.orderManagement.remoteOrder;

public interface IOrderCmd {

    public void execute(final NibblerTransportOrderEntry client);
}
