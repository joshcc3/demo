package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class SubmitOrderCmd implements IOrderCmd {

    private final String username;
    private final String symbol;
    private final BookSide side;
    private final RemoteOrderType orderType;
    private final String tag;
    private final long price;
    private final int qty;

    public SubmitOrderCmd(final String username, final String symbol, final BookSide side, final RemoteOrderType orderType,
            final String tag, final long price, final int qty) {

        this.username = username;
        this.symbol = symbol;
        this.side = side;
        this.orderType = orderType;
        this.tag = tag;
        this.price = price;
        this.qty = qty;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {

        client.submitOrder(username, symbol, side, orderType.orderType, orderType.algoType, tag, price, qty);
    }
}
