package com.drwtrading.london.reddal.obligations;

import com.drwtrading.london.util.Struct;

class Obligation extends Struct {
    public final double notional;
    public final double bps;
    Obligation(double notional, double bps) {
        this.notional = notional;
        this.bps = bps;
    }
}
