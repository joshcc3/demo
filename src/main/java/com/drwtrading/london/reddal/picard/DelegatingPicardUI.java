package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.reddal.symbols.DisplaySymbol;

import java.util.HashMap;
import java.util.Map;

public class DelegatingPicardUI {

    private final PicardUI fiPicardUI;
    private final PicardUI fcPicardUI;
    private final PicardUI emPicardUI;
    private final PicardUI dmPicardUI;

    private final Map<String, PicardUI> isinToUI;

    private final PicardUI defaultUI;

    public DelegatingPicardUI(final PicardUI fiPicardUI, final PicardUI fcPicardUI, final PicardUI emPicardUI, final PicardUI dmPicardUI) {

        this.fiPicardUI = fiPicardUI;
        this.fcPicardUI = fcPicardUI;
        this.emPicardUI = emPicardUI;
        this.dmPicardUI = dmPicardUI;
        isinToUI = new HashMap<>();
        defaultUI = dmPicardUI;
    }

    public void addFIIsin(final String isin) {
        isinToUI.put(isin, fiPicardUI);
    }

    public void addFCIsin(final String isin) {
        isinToUI.put(isin, fcPicardUI);
    }

    public void addEMIsin(final String isin) {
        isinToUI.put(isin, emPicardUI);
    }

    public void addDMIsin(final String isin) {
        isinToUI.put(isin, dmPicardUI);
    }

    public void addPicardRow(final PicardRowWithInstID row) {
        final PicardUI picardUI = isinToUI.getOrDefault(row.instrumentID.isin, defaultUI);
        picardUI.addPicardRow(row);
    }

    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {
        fiPicardUI.setDisplaySymbol(displaySymbol);
        fcPicardUI.setDisplaySymbol(displaySymbol);
        emPicardUI.setDisplaySymbol(displaySymbol);
        dmPicardUI.setDisplaySymbol(displaySymbol);
    }

    public long flush() {
        final long delay1 = fiPicardUI.flush();
        final long delay2 = emPicardUI.flush();
        final long delay3 = dmPicardUI.flush();
        final long delay4 = fcPicardUI.flush();
        return Math.min(delay4, Math.min(delay3, Math.min(delay1, delay2)));
    }

}
