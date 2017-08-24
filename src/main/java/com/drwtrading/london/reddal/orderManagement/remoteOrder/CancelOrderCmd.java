package com.drwtrading.london.reddal.orderManagement.remoteOrder;

public class CancelOrderCmd implements IOrderCmd {

    private final String username;
    private final boolean isUserLogin;
    private final int chainID;
    private final String symbol;

    public CancelOrderCmd(final String username, final boolean isUserLogin, final int chainID, final String symbol) {

        this.username = username;
        this.isUserLogin = isUserLogin;

        this.chainID = chainID;
        this.symbol = symbol;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {

        client.cancelOrder(username, isUserLogin, chainID, symbol);
    }
}
