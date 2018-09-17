package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.reddal.ladders.model.BookHTMLRow;

public class LadderBoardRow {

    public final String formattedPrice;
    public final BookHTMLRow htmlKeys;

    public LadderBoardRow(final String formattedPrice, final BookHTMLRow htmlKeys) {
        this.formattedPrice = formattedPrice;
        this.htmlKeys = htmlKeys;
    }
}
