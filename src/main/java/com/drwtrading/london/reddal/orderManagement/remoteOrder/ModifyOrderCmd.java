package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class ModifyOrderCmd implements IOrderCmd {

    private final String username;
    private final int chainID;
    private final String symbol;
    private final BookSide side;
    private final RemoteOrderType orderType;
    private final String tag;

    private final long fromPrice;
    private final int fromQty;

    private final long toPrice;
    private final int toQty;

    public ModifyOrderCmd(final String username, final int chainID, final String symbol, final BookSide side,
            final RemoteOrderType orderType, final String tag, final long fromPrice, final int fromQty, final long toPrice,
            final int toQty) {

        this.username = username;
        this.chainID = chainID;
        this.symbol = symbol;
        this.side = side;
        this.orderType = orderType;
        this.tag = tag;

        this.fromPrice = fromPrice;
        this.fromQty = fromQty;

        this.toPrice = toPrice;
        this.toQty = toQty;
    }

    @Override
    public void execute(final NibblerTransportOrderEntry client) {

        client.modifyOrder(username, chainID, symbol, side, orderType.orderType, orderType.algoType, tag, fromPrice, fromQty, toPrice,
                toQty);
    }
}
