package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.indy.transport.data.Source;
import com.drwtrading.london.reddal.symbols.SymbolIndyData;

public class InstrumentMetaData {
    public final InstrumentID instrumentID;

    private Source indyDefSource;
    private String indyDefName;

    public InstrumentMetaData(final InstrumentID instrumentID) {this.instrumentID = instrumentID;}

    public void setSymbolIndyData(final SymbolIndyData symbolIndyData) {
        this.indyDefName = symbolIndyData.description;
        this.indyDefSource = symbolIndyData.source;
    }

    public Source getIndyDefSource() {
        return indyDefSource;
    }

    public String getIndyDefName() {
        return indyDefName;
    }

    public String getDescription() {
        return getIndyDefName();
    }
}
