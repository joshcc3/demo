package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;

public enum RemoteOrderType {

    MANUAL(OrderType.LIMIT, AlgoType.MANUAL, eeif.execution.RemoteOrderType.MANUAL),
    IOC(OrderType.IOC, AlgoType.MANUAL, eeif.execution.RemoteOrderType.IOC),
    GTC(OrderType.GTC, AlgoType.MANUAL, eeif.execution.RemoteOrderType.GTC),
    BATS_PEGGED_DARK_BOOK_ONLY(OrderType.DARK_PEGGED, AlgoType.MANUAL, eeif.execution.RemoteOrderType.BATS_PEGGED_DARK_BOOK_ONLY),
    MKT_CLOSE(OrderType.MKT_CLOSE, AlgoType.MANUAL, eeif.execution.RemoteOrderType.MKT_CLOSE),
    HIDDEN(OrderType.HIDDEN_LIMIT, AlgoType.MANUAL, eeif.execution.RemoteOrderType.HIDDEN),
    HIDDEN_TICKTAKER(OrderType.LIMIT, AlgoType.HIDDEN_TICK_TAKER, eeif.execution.RemoteOrderType.HIDDEN_TICKTAKER),
    MARKET(OrderType.MARKET, AlgoType.MANUAL, eeif.execution.RemoteOrderType.MARKET),

    HAWK(OrderType.LIMIT, AlgoType.HAWK, eeif.execution.RemoteOrderType.HAWK),
    TAKER(OrderType.LIMIT, AlgoType.TAKER, eeif.execution.RemoteOrderType.TAKER);

    final OrderType orderType;
    final AlgoType algoType;

    final eeif.execution.RemoteOrderType remoteOrderType;

    private RemoteOrderType(final OrderType orderType, final AlgoType algoType, final eeif.execution.RemoteOrderType remoteOrderType) {

        this.orderType = orderType;
        this.algoType = algoType;

        this.remoteOrderType = remoteOrderType;
    }
}
