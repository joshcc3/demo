package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;

import java.util.Set;

class NibblerSymbolHandler {

    final int priority;
    final String nibblerName;

    final Set<OrderType> supportedOrderTypes;

    final NibblerTransportOrderEntry orderEntry;

    NibblerSymbolHandler(final int priority, final String nibblerName, final Set<OrderType> supportedOrderTypes,
            final NibblerTransportOrderEntry orderEntry) {

        this.priority = priority;
        this.nibblerName = nibblerName;

        this.supportedOrderTypes = supportedOrderTypes;

        this.orderEntry = orderEntry;
    }
}
