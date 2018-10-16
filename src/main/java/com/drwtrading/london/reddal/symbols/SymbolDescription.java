package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;

public class SymbolDescription {
    public final InstrumentID instrumentID;
    public final String symbol;
    public final String description;
    SymbolDescription(InstrumentID instrumentID, String symbol, String description) {
        this.instrumentID = instrumentID;
        this.symbol = symbol;
        this.description = description;
    }
}
