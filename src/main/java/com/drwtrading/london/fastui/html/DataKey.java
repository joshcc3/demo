package com.drwtrading.london.fastui.html;

public enum DataKey {

    PRICE("price"),
    ORDER("orderKeys"),
    EEIF("eeifKeys");

    public final String key;

    private DataKey(final String key) {
        this.key = key;
    }
}
