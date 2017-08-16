package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import eeif.execution.RemoteAutoCancelOrder;
import eeif.execution.RemoteCancelOrder;
import eeif.execution.RemoteOrder;
import eeif.execution.RemoteOrderManagementCommand;
import eeif.execution.Side;

public class CancelOrderCmd implements IOrderCmd {

    private final String username;
    private final boolean isUserLogin;
    private final int chainID;
    private final String symbol;

    private final BookSide side;
    private final RemoteOrderType orderType;
    private final String tag;
    private final long price;
    private final int qty;

    public CancelOrderCmd(final String username, final boolean isUserLogin, final int chainID, final String symbol, final BookSide side,
            final RemoteOrderType orderType, final String tag, final long price, final int qty) {

        this.username = username;
        this.isUserLogin = isUserLogin;

        this.chainID = chainID;
        this.symbol = symbol;

        this.side = side;
        this.orderType = orderType;
        this.tag = tag;
        this.price = price;
        this.qty = qty;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {

        client.cancelOrder(username, isUserLogin, chainID, symbol);
    }

    @Override
    public RemoteOrderManagementCommand createRemoteOrderManager(final String serverName) {

        final Side execSide = BookSide.BID == side ? Side.BID : Side.OFFER;
        final RemoteOrder order = new RemoteOrder(symbol, execSide, price, qty, orderType.remoteOrderType, true, tag);
        if (isUserLogin) {
            return new RemoteCancelOrder(serverName, username, chainID, order);
        } else {
            return new RemoteAutoCancelOrder(serverName, username, chainID, order);
        }
    }
}
