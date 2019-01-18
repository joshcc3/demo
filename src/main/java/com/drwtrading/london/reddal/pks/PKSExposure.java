package com.drwtrading.london.reddal.pks;

import java.util.Set;

public class PKSExposure {

    public final Set<String> symbols;
    public final double exposure;
    public final double position;

    PKSExposure(final Set<String> symbols, final double exposure, final double position) {

        this.symbols = symbols;
        this.exposure = exposure;
        this.position = position;
    }
}
