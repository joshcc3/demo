package com.drwtrading.london.reddal.safety;

import com.drwtrading.london.util.Struct;

public class ServerTradingStatus extends Struct {

    public final String server;
    public final boolean isWorkingOrderConnected;
    public final boolean isTradingConnection;

    public ServerTradingStatus(final String server, final boolean isWorkingOrderConnected, final boolean isTradingConnection) {

        this.server = server;
        this.isWorkingOrderConnected = isWorkingOrderConnected;
        this.isTradingConnection = isTradingConnection;
    }
}
