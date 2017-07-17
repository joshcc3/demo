package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;

public class UltimateParentMapping {

    public final String isin;
    public final InstrumentID parentID;

    public UltimateParentMapping(final String isin, final InstrumentID parentID) {

        this.isin = isin;
        this.parentID = parentID;
    }
}