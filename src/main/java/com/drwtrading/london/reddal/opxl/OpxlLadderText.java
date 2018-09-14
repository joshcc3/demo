package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.reddal.fastui.html.FreeTextCell;

public class OpxlLadderText {

    public final String symbol;
    public final FreeTextCell cell;
    public final String text;

    OpxlLadderText(final String symbol, final FreeTextCell cell, final String text) {

        this.symbol = symbol;
        this.cell = cell;
        this.text = text;
    }
}
