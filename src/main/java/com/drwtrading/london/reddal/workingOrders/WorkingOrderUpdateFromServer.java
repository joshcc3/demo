package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.util.Struct;

public class WorkingOrderUpdateFromServer extends Struct {

    public final String fromServer;
    public final WorkingOrderUpdate value;

    public WorkingOrderUpdateFromServer(final String fromServer, final WorkingOrderUpdate value) {
        this.fromServer = fromServer;
        this.value = value;
    }

    public String key() {
        return fromServer + '_' + value.getChainId();
    }
}
