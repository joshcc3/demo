package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;

import java.util.HashMap;
import java.util.Map;

public enum RemoteOrderType {

    MANUAL(OrderType.LIMIT, AlgoType.MANUAL),
    IOC(OrderType.IOC, AlgoType.MANUAL),
    GTC(OrderType.GTC, AlgoType.MANUAL),
    MIDPOINT(OrderType.DARK_PEGGED, AlgoType.MANUAL),
    MKT_CLOSE(OrderType.MKT_CLOSE, AlgoType.MANUAL),
    HIDDEN(OrderType.HIDDEN_LIMIT, AlgoType.MANUAL),
    MARKET(OrderType.MARKET, AlgoType.MANUAL),

    HAWK(OrderType.LIMIT, AlgoType.HAWK),
    TAKER(OrderType.LIMIT, AlgoType.TAKER);

    public final OrderType orderType;
    public final AlgoType algoType;

    private RemoteOrderType(final OrderType orderType, final AlgoType algoType) {

        this.orderType = orderType;
        this.algoType = algoType;
    }

    private static final Map<String, RemoteOrderType> ORDER_TYPES;

    static {
        ORDER_TYPES = new HashMap<>();

        for (final RemoteOrderType orderType : RemoteOrderType.values()) {
            ORDER_TYPES.put(orderType.name(), orderType);
        }
    }

    public static RemoteOrderType get(final String name) {
        return ORDER_TYPES.get(name);
    }

    public static RemoteOrderType getOrDefault(final String name, final RemoteOrderType defaultResult) {
        return ORDER_TYPES.getOrDefault(name, defaultResult);
    }
}
