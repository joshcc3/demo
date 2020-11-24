package com.drwtrading.london.reddal.stacks.opxl;

import com.drwtrading.london.eeif.utils.staticData.InstType;

class OpxlStrategyData {

    private final InstType instType;
    private final String symbol;
    private final Object[] row;

    private boolean isLean = false;
    private boolean isQuote = false;

    OpxlStrategyData(final InstType instType, final String symbol, final Object[] row) {
        this.instType = instType;
        this.symbol = symbol;
        this.row = row;
    }

    boolean setLean() {
        final boolean wasLean = isLean;
        isLean = true;
        row[2] = true;
        return !wasLean;
    }

    boolean setQuote() {
        final boolean wasQuote = isQuote;
        isQuote = true;
        row[3] = true;
        return !wasQuote;
    }
}
