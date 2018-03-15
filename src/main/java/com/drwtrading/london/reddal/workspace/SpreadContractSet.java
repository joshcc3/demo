package com.drwtrading.london.reddal.workspace;

import com.drwtrading.london.util.Struct;

import java.util.HashMap;
import java.util.Map;

public class SpreadContractSet extends Struct {

    public final String symbol;

    public final String nextContract;
    public final String contractAfterNext;

    public final String frontMonth;
    public final String backMonth;
    public final String spread;

    public final String leanSymbol;
    public final String stackSymbol;
    public final String parentSymbol;

    private final Map<String, String> nextSymbols;

    SpreadContractSet(final String symbol, final String frontMonth, final String backMonth, final String leanSymbol,
            final String stackSymbol, final String parentSymbol) {

        this.symbol = symbol;

        if (backMonth != null) {
            this.frontMonth = symbol;
            this.backMonth = backMonth;
            this.spread = this.frontMonth + '-' + this.backMonth;
            this.nextContract = this.backMonth;
            this.contractAfterNext = this.spread;
        } else if (frontMonth != null) {
            this.frontMonth = frontMonth;
            this.backMonth = symbol;
            this.spread = this.frontMonth + '-' + this.backMonth;
            this.nextContract = this.spread;
            this.contractAfterNext = this.frontMonth;
        } else {
            this.frontMonth = symbol;
            this.backMonth = symbol;
            this.spread = symbol;
            this.nextContract = symbol;
            this.contractAfterNext = symbol;
        }

        this.leanSymbol = leanSymbol;
        this.stackSymbol = stackSymbol;
        this.parentSymbol = parentSymbol;

        this.nextSymbols = new HashMap<>();

        String prevSymbol = symbol;
        prevSymbol = addSymbolNext(prevSymbol, nextContract);
        prevSymbol = addSymbolNext(prevSymbol, contractAfterNext);
        if (!symbol.equals(leanSymbol)) {
            prevSymbol = addSymbolNext(prevSymbol, leanSymbol);
        }
        prevSymbol = addSymbolNext(prevSymbol, parentSymbol);
        addSymbolNext(prevSymbol, symbol);
    }

    private String addSymbolNext(final String prevSymbol, final String nextSymbol) {
        if (null == nextSymbol || nextSymbol.equals(prevSymbol)) {
            return prevSymbol;
        } else {
            nextSymbols.put(prevSymbol, nextSymbol);
            return nextSymbol;
        }
    }

    public String next(final String from) {
        return nextSymbols.get(from);
    }
}
