package com.drwtrading.london.reddal.orderManagement.oe;

import com.drwtrading.london.util.Struct;
import drw.eeif.eeifoe.OrderEntryCommand;

public class OrderEntryCommandToServer extends Struct {

    public final String server;
    public final OrderEntryCommand command;

    public OrderEntryCommandToServer(final String server, final OrderEntryCommand command) {
        this.server = server;
        this.command = command;
    }
}
