package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.eeif.utils.Constants;

public class StackPanelRow {

    final int rowID;
    public final StackHTMLRow htmlData;

    private long priceKey;
    private double humanPriceKey;
    private String formattedPrice;

    private long bidQuote;
    private long bidPicard;

    private long askPicard;
    private long askQuote;

    StackPanelRow(final int rowID) {

        this.rowID = rowID;
        this.htmlData = new StackHTMLRow(rowID);

        this.priceKey = 0;
        this.humanPriceKey = 0d;
        this.formattedPrice = null;

        clear();
    }

    void clear() {

        this.bidQuote = 0;
        this.bidPicard = 0;

        this.askPicard = 0;
        this.askQuote = 0;
    }

    void setPrice(final long price, final String formattedPrice) {

        this.priceKey = price;
        this.humanPriceKey = price / (double) Constants.NORMALISING_FACTOR;
        this.formattedPrice = formattedPrice;
    }

    public long getPrice() {
        return priceKey;
    }

    String getRawFormattedPrice() {
        return formattedPrice;
    }

    public boolean setBidQuoteQty(final long qty) {

        if (this.bidQuote != qty) {
            this.bidQuote = qty;
            return true;
        } else {
            return false;
        }
    }

    public boolean setBidPicardQty(final long qty) {

        if (this.bidPicard != qty) {
            this.bidPicard = qty;
            return true;
        } else {
            return false;
        }
    }

    public boolean setAskPicardQty(final long qty) {

        if (this.askPicard != qty) {
            this.askPicard = qty;
            return true;
        } else {
            return false;
        }
    }

    public boolean setAskQuoteQty(final long qty) {

        if (this.askQuote != qty) {
            this.askQuote = qty;
            return true;
        } else {
            return false;
        }
    }
}
