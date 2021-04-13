package com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.Tag;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.RemoteOrderServerRouter;
import org.jetlang.channels.Publisher;

public class ModifyOrderCmd implements IOrderCmd {

    private final String toServer;
    private final Publisher<LadderClickTradingIssue> rejectChannel;

    private final User user;
    private final int chainID;
    private final String symbol;
    private final BookSide side;
    private final OrderType orderType;
    private final AlgoType algoType;
    private final Tag tag;

    private final long fromPrice;
    private final int fromQty;

    private final long toPrice;
    private final int toQty;

    public ModifyOrderCmd(final String toServer, final Publisher<LadderClickTradingIssue> rejectChannel, final User user, final int chainID,
            final String symbol, final BookSide side, final OrderType orderType, final AlgoType algoType, final Tag tag,
            final long fromPrice, final int fromQty, final long toPrice, final int toQty) {

        this.toServer = toServer;
        this.rejectChannel = rejectChannel;

        this.user = user;
        this.chainID = chainID;
        this.symbol = symbol;
        this.side = side;
        this.orderType = orderType;
        this.algoType = algoType;
        this.tag = tag;

        this.fromPrice = fromPrice;
        this.fromQty = fromQty;

        this.toPrice = toPrice;
        this.toQty = toQty;
    }

    @Override
    public void route(final RemoteOrderServerRouter router) {
        router.routeCmd(toServer, this);
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {

        client.modifyOrder(rejectChannel, user, chainID, symbol, side, orderType, algoType, tag, fromPrice, fromQty, toPrice, toQty);
    }

    @Override
    public void rejectMsg(final String msg) {

        final LadderClickTradingIssue reject = new LadderClickTradingIssue(symbol, msg);
        rejectChannel.publish(reject);
    }
}
