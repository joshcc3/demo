package com.drwtrading.london.reddal.orderentry;

import com.drwtrading.london.photons.eeifoe.Update;
import com.drwtrading.london.util.Struct;

public class UpdateFromServer extends Struct implements OrderEntryFromServer {

    public final String server;
    public final Update update;

    public UpdateFromServer(final String server, final Update update) {
        this.server = server;
        this.update = update;
    }

    public String key() {
        return "EEIFOE_" + server + '_' + update.getSystemOrderId();
    }

    public String getSymbol() {
        return update.getOrder().getSymbol();
    }
}
