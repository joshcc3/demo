package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;

public enum RemoteOrderType {

    MANUAL(OrderType.LIMIT, AlgoType.MANUAL),
    IOC(OrderType.IOC, AlgoType.MANUAL),
    GTC(OrderType.GTC, AlgoType.MANUAL),
    BATS_PEGGED_DARK_BOOK_ONLY(OrderType.DARK_PEGGED, AlgoType.MANUAL),
    MKT_CLOSE(OrderType.MKT_CLOSE, AlgoType.MANUAL),
    HIDDEN(OrderType.HIDDEN_LIMIT, AlgoType.MANUAL),
    HIDDEN_TICKTAKER(OrderType.LIMIT, AlgoType.HIDDEN_TICK_TAKER),
    MARKET(OrderType.MARKET, AlgoType.MANUAL),

    HAWK(OrderType.LIMIT, AlgoType.HAWK),
    TAKER(OrderType.LIMIT, AlgoType.TAKER);

    public final OrderType orderType;
    public final AlgoType algoType;

    private RemoteOrderType(final OrderType orderType, final AlgoType algoType) {

        this.orderType = orderType;
        this.algoType = algoType;
    }
}
