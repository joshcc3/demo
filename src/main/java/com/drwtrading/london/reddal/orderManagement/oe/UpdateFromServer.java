package com.drwtrading.london.reddal.orderManagement.oe;

import com.drwtrading.london.photons.eeifoe.Update;
import com.drwtrading.london.util.Struct;

public class UpdateFromServer extends Struct implements OrderEntryFromServer {

    public final String server;
    public final Update update;
    public final String key;
    public final String symbol;

    public UpdateFromServer(final String server, final Update update) {
        this.server = server;
        this.update = update;
        key = "EEIFOE_" + server + '_' + update.getSystemOrderId();
        symbol = update.getOrder().getSymbol();
    }

}
