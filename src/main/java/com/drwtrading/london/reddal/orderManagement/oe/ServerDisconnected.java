package com.drwtrading.london.reddal.orderManagement.oe;

import com.drwtrading.london.util.Struct;

public class ServerDisconnected extends Struct implements OrderEntryFromServer {

    public final String server;

    public ServerDisconnected(final String server) {
        this.server = server;
    }
}
