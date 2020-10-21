package com.drwtrading.london.reddal.orderManagement.oe;

import com.drwtrading.london.util.Struct;
import drw.eeif.eeifoe.OrderEntryCommand;
import org.jetlang.channels.Publisher;

import java.util.Set;

public class OrderEntrySymbolChannel extends Struct {

    public final String symbol;
    public final Publisher<OrderEntryCommand> publisher;
    public final Set<ManagedOrderType> supportedTypes;

    public OrderEntrySymbolChannel(final String symbol, final Publisher<OrderEntryCommand> publisher,
            final Set<ManagedOrderType> supportedTypes) {

        this.symbol = symbol;
        this.publisher = publisher;
        this.supportedTypes = supportedTypes;
    }
}
