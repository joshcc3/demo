package com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds;

import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderServerRouter;
import org.jetlang.channels.Publisher;

public class CancelOrderCmd implements IOrderCmd {

    private final String toServer;
    private final Publisher<LadderClickTradingIssue> rejectChannel;

    private final String username;
    private final boolean isAuto;
    private final int chainID;
    private final String symbol;

    public CancelOrderCmd(final String toServer, final Publisher<LadderClickTradingIssue> rejectChannel, final String username,
            final boolean isAuto, final int chainID, final String symbol) {

        this.toServer = toServer;
        this.rejectChannel = rejectChannel;

        this.username = username;
        this.isAuto = isAuto;

        this.chainID = chainID;
        this.symbol = symbol;
    }

    @Override
    public void route(final RemoteOrderServerRouter router) {
        router.routeCmd(toServer, this);
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {
        client.cancelOrder(rejectChannel, username, isAuto, chainID, symbol);
    }

    @Override
    public void rejectMsg(final String msg) {

        final LadderClickTradingIssue reject = new LadderClickTradingIssue(symbol, msg);
        rejectChannel.publish(reject);
    }
}
