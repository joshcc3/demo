package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.protocols.photon.execution.WorkingOrderEvent;
import com.drwtrading.london.util.Struct;

public class WorkingOrderEventFromServer extends Struct {

    public final String fromServer;
    public final WorkingOrderEvent value;

    public WorkingOrderEventFromServer(final String fromServer, final WorkingOrderEvent value) {
        this.fromServer = fromServer;
        this.value = value;
    }
}
