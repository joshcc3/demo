package com.drwtrading.london.reddal.pks;

import java.util.Set;

public class PKSExposure {

    public final Set<String> symbols;

    public final double dryExposure;
    public final double dryPosition;

    public final double dripExposure;
    public final double dripPosition;

    PKSExposure(final Set<String> symbols, final double dryExposure, final double dryPosition, final double dripExposure,
            final double dripPosition) {

        this.symbols = symbols;
        this.dryExposure = dryExposure;
        this.dryPosition = dryPosition;
        this.dripExposure = dripExposure;
        this.dripPosition = dripPosition;
    }

    public double getCombinedPosition() {
        return dryPosition + dripPosition;
    }
}
