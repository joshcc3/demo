package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.util.Struct;

import java.util.HashMap;
import java.util.Map;

public class SpreadContractSet extends Struct {

    public final String symbol;

    public final String backMonth;
    public final String spread;

    public final String leanSymbol;
    public final String stackSymbol;
    public final String parentSymbol;

    private final Map<String, String> nextSymbols;

    public SpreadContractSet(final String symbol, final String backMonth, final String spread, final String leanSymbol,
            final String stackSymbol, final String parentSymbol) {

        this.symbol = symbol;

        this.backMonth = backMonth;
        this.spread = spread;

        this.leanSymbol = leanSymbol;
        this.stackSymbol = stackSymbol;
        this.parentSymbol = parentSymbol;

        this.nextSymbols = new HashMap<>();

        String prevSymbol = symbol;
        prevSymbol = addSymbolNext(prevSymbol, backMonth);
        prevSymbol = addSymbolNext(prevSymbol, spread);
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
