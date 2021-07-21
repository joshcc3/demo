package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;

import java.util.HashMap;
import java.util.Map;

public class DelegatingPicardUI {

    private final Map<String, PicardUI> symbolToUI;

    private final Map<StackCommunity, PicardUI> picardUIs;
    private final PicardUI allView;

    public DelegatingPicardUI(final Map<StackCommunity, PicardUI> picardUIs, final PicardUI allView) {

        this.picardUIs = picardUIs;
        this.allView = allView;
        this.symbolToUI = new HashMap<>();
    }

    public void addSymbol(final StackCommunity community, final String symbol) {
        final PicardUI picardUI = picardUIs.get(community);
        if (null != picardUI) {
            symbolToUI.put(symbol, picardUI);
        }
    }

    public void addPicardRow(final PicardRowWithInstID row) {
        final PicardUI picardUI = symbolToUI.get(row.symbol);
        if (null != picardUI) {
            picardUI.addPicardRow(row);
        }
        allView.addPicardRow(row);
    }

    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {
        for (final PicardUI ui : picardUIs.values()) {
            ui.setDisplaySymbol(displaySymbol);
        }
        allView.setDisplaySymbol(displaySymbol);
    }

    public long flush() {
        long nextDelay = Long.MAX_VALUE;
        for (final PicardUI ui : picardUIs.values()) {
            nextDelay = Math.min(nextDelay, ui.flush());
        }
        nextDelay = Math.min(allView.flush(), nextDelay);
        return nextDelay;
    }

}
