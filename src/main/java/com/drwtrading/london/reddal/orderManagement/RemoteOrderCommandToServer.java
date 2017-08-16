package com.drwtrading.london.reddal.orderManagement;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.util.Struct;

public class RemoteOrderCommandToServer extends Struct {

    public final String toServer;
    public final IOrderCmd value;

    public RemoteOrderCommandToServer(final String toServer, final IOrderCmd value) {
        this.toServer = toServer;
        this.value = value;
    }
}
