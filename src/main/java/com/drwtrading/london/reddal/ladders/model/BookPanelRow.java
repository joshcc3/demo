package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.reddal.ladders.PricingMode;

public class BookPanelRow {

    final int rowID;
    public final BookHTMLRow htmlData;

    private long priceKey;
    private double humanPriceKey;
    private String formattedPrice;

    private long workingOrderQty;
    private long bidQty;

    private PricingMode priceType;
    private double displayPrice;

    private long askQty;
    private long volume;
    private long lastTradePriceVolume;

    BookPanelRow(final int rowID) {

        this.rowID = rowID;
        this.htmlData = new BookHTMLRow(rowID);

        this.priceKey = 0;
        this.humanPriceKey = 0d;
        this.formattedPrice = null;

        clear();
    }

    void clear() {

        this.workingOrderQty = 0;
        this.bidQty = 0;

        this.priceType = null;
        this.displayPrice = 0d;

        this.askQty = 0;
        this.volume = 0;
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

    boolean setRawDisplayPrice() {
        return setDisplayPrice(PricingMode.RAW, humanPriceKey);
    }

    boolean setDisplayPrice(final PricingMode priceType, final double displayPrice) {

        if (this.priceType != priceType || Constants.EPSILON < Math.abs(displayPrice - this.displayPrice)) {

            this.priceType = priceType;
            this.displayPrice = displayPrice;
            return true;
        } else {
            return false;
        }
    }

    boolean setWorkingOrderQty(final long workingOrderQty) {

        if (this.workingOrderQty != workingOrderQty) {
            this.workingOrderQty = workingOrderQty;
            return true;
        } else {
            return false;
        }
    }

    boolean setBidQty(final long qty) {

        if (this.bidQty != qty) {
            this.bidQty = qty;
            return true;
        } else {
            return false;
        }
    }

    boolean setAskQty(final long qty) {

        if (this.askQty != qty) {
            this.askQty = qty;
            return true;
        } else {
            return false;
        }
    }

    boolean setVolume(final Long volume) {

        if (this.volume != volume) {
            this.volume = volume;
            return true;
        } else {
            return false;
        }
    }

    boolean setLastTradePriceVolume(final Long volume) {

        if (this.lastTradePriceVolume != volume) {
            this.lastTradePriceVolume = volume;
            return true;
        } else {
            return false;
        }
    }

    /*
        <div id="row_template" class="book row template">
            <div class="order column"></div>
            <div class="bid column"></div>
            <div class="price column"></div>
            <div class="offer column"></div>
            <div class="trade column invisible"></div>
            <div class="volume column"></div>
            <div class="text column invisible"></div>
        </div>
     */
}
