package com.drwtrading.london.reddal.workspace;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.util.Struct;

public class LeanDef extends Struct {
    public final String symbol;
    public final InstrumentID instID;
    public final InstType instType;

    LeanDef(String symbol, InstrumentID leanDef, InstType instType) {
        this.symbol = symbol;
        this.instID = leanDef;
        this.instType = instType;
    }
}
