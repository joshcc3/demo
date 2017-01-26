package com.drwtrading.london.reddal.pks;

public class PKSExposure {

    public final String symbol;
    public final double exposure;
    public final double position;

    public PKSExposure(final String symbol, final double exposure, final double position) {

        this.symbol = symbol;
        this.exposure = exposure;
        this.position = position;
    }
}
