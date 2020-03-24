package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.marketData.book.IInstrument;

public class SymbolReferencePrice {

    public final IInstrument inst;
    public final long yestClosePrice;

    public SymbolReferencePrice(final IInstrument inst, final long yestClosePrice) {

        this.inst = inst;
        this.yestClosePrice = yestClosePrice;
    }
}
