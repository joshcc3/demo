package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.util.Struct;

public class DisplaySymbol extends Struct {

    public final String marketDataSymbol;
    public final String displaySymbol;

    public DisplaySymbol(final String marketDataSymbol, final String displaySymbol) {

        this.marketDataSymbol = marketDataSymbol;
        this.displaySymbol = displaySymbol;
    }
}
