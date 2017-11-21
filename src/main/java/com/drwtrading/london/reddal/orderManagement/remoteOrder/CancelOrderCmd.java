package com.drwtrading.london.reddal.orderManagement.remoteOrder;

public class CancelOrderCmd implements IOrderCmd {

    private final String username;
    private final boolean isAuto;
    private final int chainID;
    private final String symbol;

    public CancelOrderCmd(final String username, final boolean isAuto, final int chainID, final String symbol) {

        this.username = username;
        this.isAuto = isAuto;

        this.chainID = chainID;
        this.symbol = symbol;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {

        client.cancelOrder(username, isAuto, chainID, symbol);
    }
}
