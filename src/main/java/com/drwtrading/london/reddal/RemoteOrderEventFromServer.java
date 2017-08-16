package com.drwtrading.london.reddal;

import com.drwtrading.london.util.Struct;
import eeif.execution.RemoteOrderManagementEvent;

public class RemoteOrderEventFromServer extends Struct {

    public final String fromServer;
    public final RemoteOrderManagementEvent value;

    public RemoteOrderEventFromServer(final String fromServer, final RemoteOrderManagementEvent value) {
        this.fromServer = fromServer;
        this.value = value;
    }
}
