package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;

public class LadderTextUpdate {

    public final String symbol;
    public final ReddalFreeTextCell cell;
    public final String text;
    public final String description;

    public LadderTextUpdate(final String symbol, final ReddalFreeTextCell cell, final String text, final String description) {

        this.symbol = symbol;
        this.cell = cell;
        this.text = text;
        this.description = description;
    }
}
