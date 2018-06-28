package com.drwtrading.london.reddal.obligations;

import drw.london.json.JSONGenerator;
import drw.london.json.Jsonable;

import java.io.IOException;

class Obligation implements Jsonable {
    public final double notional;
    public final double bps;

    Obligation(double notional, double bps) {
        this.notional = notional;
        this.bps = bps;
    }

    @Override
    public void toJson(Appendable out) throws IOException {
        JSONGenerator.jsObject(out,
                "notional", notional,
                "bps", bps);
    }
}
