package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.reddal.fastui.html.HTML;

public class BookHTMLRow {

    public final String bookBidKey;
    public final String bookAskKey;
    public final String bookSideKey;
    public final String bookPriceKey;
    public final String bookOrderKey;
    public final String bookVolumeKey;
    public final String bookTradeKey;

    public BookHTMLRow(final int row) {

        this.bookBidKey = HTML.BID + row;
        this.bookAskKey = HTML.OFFER + row;
        this.bookSideKey = HTML.SIDE + row;
        this.bookPriceKey = HTML.PRICE + row;
        this.bookOrderKey = HTML.ORDER + row;
        this.bookVolumeKey = HTML.VOLUME + row;
        this.bookTradeKey = HTML.TRADE + row;
    }
}
