package com.drwtrading.london.reddal.ladders.history;

public class SymbolSelection {

    public final String username;

    public final String symbol;
    public final boolean isStackView;

    public SymbolSelection(final String username, final String[] subscriptionArgs) {

        this.username = username;

        this.symbol = subscriptionArgs[1];
        this.isStackView = 3 < subscriptionArgs.length && "S".equals(subscriptionArgs[3]);
    }
}
