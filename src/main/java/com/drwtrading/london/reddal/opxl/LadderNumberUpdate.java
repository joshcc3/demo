package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.icepie.transport.io.LadderTextNumberUnits;
import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;
import com.drwtrading.london.reddal.ladders.PricingMode;

import java.util.EnumMap;

public class LadderNumberUpdate {

    public final String symbol;
    public final ReddalFreeTextCell cell;
    public final long value;
    public final LadderTextNumberUnits units;
    public final String description;

    public LadderNumberUpdate(final String symbol, final ReddalFreeTextCell cell, final long value, final LadderTextNumberUnits units,
            final String description) {

        this.symbol = symbol;
        this.cell = cell;
        this.value = value;
        this.units = units;
        this.description = description;
    }


}
