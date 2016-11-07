package com.drwtrading.london.reddal.ladders;

class LadderBoardRow {

    final String formattedPrice;
    final LadderHTMLRow htmlKeys;

    LadderBoardRow(final String formattedPrice, final LadderHTMLRow htmlKeys) {
        this.formattedPrice = formattedPrice;
        this.htmlKeys = htmlKeys;
    }
}
