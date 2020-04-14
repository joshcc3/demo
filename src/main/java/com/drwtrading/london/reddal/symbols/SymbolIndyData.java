package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.indy.transport.data.Source;

public class SymbolIndyData {

    public final InstrumentID instrumentID;
    public final String symbol;
    public final String description;
    public final Source source;

    SymbolIndyData(final InstrumentID instrumentID, final String symbol, final String description, final Source source) {

        this.instrumentID = instrumentID;
        this.symbol = symbol;
        this.description = description;
        this.source = source;
    }
}
