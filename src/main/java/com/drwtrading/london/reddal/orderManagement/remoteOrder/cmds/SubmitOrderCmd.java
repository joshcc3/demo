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

public class SubmitOrderCmd implements IOrderCmd {

    private final String symbol;
    private final Publisher<LadderClickTradingIssue> rejectChannel;

    private final User user;
    private final BookSide side;
    private final OrderType orderType;
    private final AlgoType algoType;
    private final Tag tag;
    private final long price;
    private final int qty;

    public SubmitOrderCmd(final String symbol, final Publisher<LadderClickTradingIssue> rejectChannel, final User user, final BookSide side,
            final OrderType orderType, final AlgoType algoType, final Tag tag, final long price, final int qty) {

        this.symbol = symbol;
        this.rejectChannel = rejectChannel;

        this.user = user;
        this.side = side;
        this.orderType = orderType;
        this.algoType = algoType;
        this.tag = tag;
        this.price = price;
        this.qty = qty;
    }

    public String getSymbol() {
        return symbol;
    }

    public OrderType getOrderType() {
        return orderType;
    }

    @Override
    public void route(final RemoteOrderServerRouter router) {
        router.submitOrder(this);
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {

        client.submitOrder(rejectChannel, user, symbol, side, orderType, algoType, tag, price, qty);
    }

    @Override
    public void rejectMsg(final String msg) {

        final LadderClickTradingIssue reject = new LadderClickTradingIssue(symbol, msg);
        rejectChannel.publish(reject);
    }
}
