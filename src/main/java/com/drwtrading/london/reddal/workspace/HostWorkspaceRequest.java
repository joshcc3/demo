package com.drwtrading.london.reddal.workspace;

public class HostWorkspaceRequest {

    public final String host;
    public final String symbol;

    public HostWorkspaceRequest(final String host, final String symbol) {
        this.host = host;
        this.symbol = symbol;
    }
}
