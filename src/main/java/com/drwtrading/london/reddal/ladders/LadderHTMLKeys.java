package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.fastui.html.HTML;

import java.util.ArrayList;

class LadderHTMLKeys {

    private final ArrayList<String> bidHTMLKeys;
    private final ArrayList<String> offerHTMLKeys;
    private final ArrayList<String> priceHTMLKeys;
    private final ArrayList<String> orderHTMLKeys;
    private final ArrayList<String> volumeHTMLKeys;
    private final ArrayList<String> tradeHTMLKeys;

    LadderHTMLKeys() {

        this.bidHTMLKeys = new ArrayList<>();
        this.offerHTMLKeys = new ArrayList<>();
        this.priceHTMLKeys = new ArrayList<>();
        this.orderHTMLKeys = new ArrayList<>();
        this.volumeHTMLKeys = new ArrayList<>();
        this.tradeHTMLKeys = new ArrayList<>();
    }

    void extendToLevels(final int levels) {

        for (int i = bidHTMLKeys.size(); i < levels; ++i) {

            bidHTMLKeys.add(i, HTML.BID + i);
            offerHTMLKeys.add(i, HTML.OFFER + i);
            priceHTMLKeys.add(i, HTML.PRICE + i);
            orderHTMLKeys.add(i, HTML.ORDER + i);
            volumeHTMLKeys.add(i, HTML.VOLUME + i);
            tradeHTMLKeys.add(i, HTML.TRADE + i);
        }
    }

    String getBidKey(final int level) {
        return bidHTMLKeys.get(level);
    }

    String getOfferKey(final int level) {
        return offerHTMLKeys.get(level);
    }

    String getPriceKey(final int level) {
        return priceHTMLKeys.get(level);
    }

    String getOrderKey(final int level) {
        return orderHTMLKeys.get(level);
    }

    String getVolumeKey(final int level) {
        return volumeHTMLKeys.get(level);
    }

    String getTradeKey(final int level) {
        return tradeHTMLKeys.get(level);
    }
}
