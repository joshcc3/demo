package com.drwtrading.london.reddal.data;

import com.drwtrading.london.util.Struct;

public class DisplaySymbol extends Struct {
    public final String marketDataSymbol;
    public final String displaySymbol;

    public DisplaySymbol(String marketDataSymbol, String displaySymbol) {
        this.marketDataSymbol = marketDataSymbol;
        this.displaySymbol = displaySymbol;
    }
}
