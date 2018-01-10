package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;

public class UltimateParentMapping {

    public final String childISIN;
    public final InstrumentID parentID;
    public final double parentToChildRatio;

    public UltimateParentMapping(final String childISIN, final InstrumentID parentID, final double parentToChildRatio) {

        this.childISIN = childISIN;
        this.parentID = parentID;
        this.parentToChildRatio = parentToChildRatio;
    }
}