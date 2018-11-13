package com.drwtrading.london.reddal.orderManagement;

import drw.london.json.JSONGenerator;
import drw.london.json.Jsonable;

import java.io.IOException;

public class NibblerTransportConnected implements Jsonable {

    public final String nibblerName;
    public final boolean isConnected;

    public NibblerTransportConnected(final String nibblerName, final boolean isConnected) {

        this.nibblerName = nibblerName;
        this.isConnected = isConnected;
    }

    @Override
    public void toJson(final Appendable appendable) throws IOException {
        JSONGenerator.jsObject(appendable, "nibbler", nibblerName, "isConnected", isConnected);
    }
}
