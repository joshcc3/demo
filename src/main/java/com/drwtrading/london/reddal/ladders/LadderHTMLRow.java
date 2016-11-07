package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.fastui.html.HTML;

public class LadderHTMLRow {

    public final String bidKey;
    public final String askKey;
    public final String priceKey;
    public final String orderKey;
    public final String volumeKey;
    public final String tradeKey;

    public LadderHTMLRow(final int row) {

        this.bidKey = HTML.BID + row;
        this.askKey = HTML.OFFER + row;
        this.priceKey = HTML.PRICE + row;
        this.orderKey = HTML.ORDER + row;
        this.volumeKey = HTML.VOLUME + row;
        this.tradeKey = HTML.TRADE + row;
    }
}
