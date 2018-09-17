package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.reddal.fastui.html.HTML;

public class StackHTMLRow {

    public final String stackBidQuoteKey;
    public final String stackBidPicardKey;
    public final String stackBidOffsetKey;

    public final String stackAskOffsetKey;
    public final String stackAskPicardKey;
    public final String stackAskQuoteKey;

    StackHTMLRow(final int row) {

        this.stackBidQuoteKey = HTML.STACK_BID_QUOTE + row;
        this.stackBidPicardKey = HTML.STACK_BID_PICARD + row;
        this.stackBidOffsetKey = HTML.STACK_BID_OFFSET + row;

        this.stackAskOffsetKey = HTML.STACK_ASK_OFFSET + row;
        this.stackAskPicardKey = HTML.STACK_ASK_PICARD + row;
        this.stackAskQuoteKey = HTML.STACK_ASK_QUOTE + row;
    }
}
