package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.reddal.fastui.html.HTML;

public class LadderHTMLRow {

    public final String bookBidKey;
    public final String bookAskKey;
    public final String bookPriceKey;
    public final String bookOrderKey;
    public final String bookVolumeKey;
    public final String bookTradeKey;

    final String stackBidQuoteKey;
    final String stackBidPicardKey;
    final String stackBidOffsetKey;

    final String stackAskOffsetKey;
    final String stackAskPicardKey;
    final String stackAskQuoteKey;

    public LadderHTMLRow(final int row) {

        this.bookBidKey = HTML.BID + row;
        this.bookAskKey = HTML.OFFER + row;
        this.bookPriceKey = HTML.PRICE + row;
        this.bookOrderKey = HTML.ORDER + row;
        this.bookVolumeKey = HTML.VOLUME + row;
        this.bookTradeKey = HTML.TRADE + row;

        this.stackBidQuoteKey = HTML.STACK_BID_QUOTE + row;
        this.stackBidPicardKey = HTML.STACK_BID_PICARD + row;
        this.stackBidOffsetKey = HTML.STACK_BID_OFFSET + row;

        this.stackAskOffsetKey = HTML.STACK_ASK_OFFSET + row;
        this.stackAskPicardKey = HTML.STACK_ASK_PICARD + row;
        this.stackAskQuoteKey = HTML.STACK_ASK_QUOTE + row;
    }
}
