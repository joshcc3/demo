package com.drwtrading.london.reddal.workingOrders;

public class WorkingOrderConnectionEstablished {

    public final String server;
    public final boolean established;

    public WorkingOrderConnectionEstablished(final String server, final boolean established) {
        this.server = server;
        this.established = established;
    }
}
