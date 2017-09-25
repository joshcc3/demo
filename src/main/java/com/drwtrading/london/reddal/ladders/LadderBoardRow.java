package com.drwtrading.london.reddal.ladders;

public class LadderBoardRow {

    public final String formattedPrice;
    public final LadderHTMLRow htmlKeys;

    public LadderBoardRow(final String formattedPrice, final LadderHTMLRow htmlKeys) {
        this.formattedPrice = formattedPrice;
        this.htmlKeys = htmlKeys;
    }
}
