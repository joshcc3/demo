package com.drwtrading.london.reddal.fastui.html;

import com.drwtrading.london.reddal.fastui.IEnumKey;

public enum DataKey implements IEnumKey {

    PRICE("price"),
    ORDER("orderKeys"),
    EEIF("eeifKeys");

    public final String key;

    private DataKey(final String key) {
        this.key = key;
    }

    @Override
    public String getHTMLKey() {
        return key;
    }
}
