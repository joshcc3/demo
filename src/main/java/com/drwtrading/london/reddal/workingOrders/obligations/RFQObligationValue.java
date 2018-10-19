package com.drwtrading.london.reddal.workingOrders.obligations;

import drw.london.json.JSONGenerator;
import drw.london.json.Jsonable;

import java.io.IOException;

class RFQObligationValue implements Jsonable {

    public final double notional;
    public final double bps;

    RFQObligationValue(final double notional, final double bps) {
        this.notional = notional;
        this.bps = bps;
    }

    @Override
    public void toJson(final Appendable out) throws IOException {
        JSONGenerator.jsObject(out, "notional", notional, "bps", bps);
    }
}
