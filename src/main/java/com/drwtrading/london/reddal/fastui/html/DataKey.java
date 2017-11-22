package com.drwtrading.london.reddal.fastui.html;

import com.drwtrading.london.reddal.fastui.IEnumKey;

public enum DataKey implements IEnumKey {

    PRICE("price"),
    QUANTITY("quantity"),
    ORDER("orderKeys"),
    EEIF("eeifKeys"),
    VOLUME_IN_FRONT("vIF"),
    ORDER_TYPE("orderType"),
    TAG("tag");

    public final String key;

    private DataKey(final String key) {
        this.key = key;
    }

    @Override
    public String getHTMLKey() {
        return key;
    }
}