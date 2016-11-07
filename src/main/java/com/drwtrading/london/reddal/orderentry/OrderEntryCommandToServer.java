package com.drwtrading.london.reddal.orderentry;

import com.drwtrading.london.photons.eeifoe.OrderEntryCommand;
import com.drwtrading.london.util.Struct;

public class OrderEntryCommandToServer extends Struct {

    public final String server;
    public final OrderEntryCommand command;

    public OrderEntryCommandToServer(String server, OrderEntryCommand command) {
        this.server = server;
        this.command = command;
    }
}
