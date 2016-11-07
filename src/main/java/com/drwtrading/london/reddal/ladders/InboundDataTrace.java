package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.util.Struct;

import java.util.Map;

public class InboundDataTrace extends Struct {

    public final String remote;
    public final String user;
    public final String[] inbound;
    public final Map<String, String> dataArg;

    public InboundDataTrace(final String remote, final String user, final String[] inbound, final Map<String, String> dataArg) {
        this.remote = remote;
        this.user = user;
        this.inbound = inbound;
        this.dataArg = dataArg;
    }
}
