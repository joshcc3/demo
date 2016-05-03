package com.drwtrading.london.reddal;

import com.drwtrading.london.util.Struct;

public class ChixSymbolPair extends Struct {

    public final String primarySymbol;
    public final String chixSymbol;

    public ChixSymbolPair(final String primarySymbol, final String chixSymbol) {

        this.primarySymbol = primarySymbol;
        this.chixSymbol = chixSymbol;
    }
}
