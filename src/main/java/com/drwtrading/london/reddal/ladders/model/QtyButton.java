package com.drwtrading.london.reddal.ladders.model;

import java.util.HashMap;
import java.util.Map;

public enum QtyButton {

    ONE("btn_qty_1"),
    TWO("btn_qty_2"),
    THREE("btn_qty_3"),
    FOUR("btn_qty_4"),
    FIVE("btn_qty_5"),
    SIX("btn_qty_6");

    public final String htmlKey;

    private QtyButton(final String htmlKey) {
        this.htmlKey = htmlKey;
    }

    private static final Map<String, QtyButton> BUTTONS;

    static {

        BUTTONS = new HashMap<>();
        for (final QtyButton button : QtyButton.values()) {
            BUTTONS.put(button.htmlKey, button);
        }
    }

    public static QtyButton getButtonFromHTML(final String htmlKey) {
        return BUTTONS.get(htmlKey);
    }
}
