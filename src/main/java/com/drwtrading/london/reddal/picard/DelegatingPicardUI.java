package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;

import java.util.HashMap;
import java.util.Map;

public class DelegatingPicardUI {

    private final PicardUI fiPicardUI;
    private final PicardUI dmPicardUI;

    private final Map<InstrumentID, PicardUI> instIDToUI;

    private final PicardUI defaultUI;

    public DelegatingPicardUI(final PicardUI fiPicardUI, final PicardUI dmPicardUI) {

        this.fiPicardUI = fiPicardUI;
        this.dmPicardUI = dmPicardUI;
        instIDToUI = new HashMap<>();
        defaultUI = dmPicardUI;
    }

    public void addFIInstrumentID(final InstrumentID instrumentID) {
        instIDToUI.put(instrumentID, fiPicardUI);
    }

    public void addDMInstrumentID(final InstrumentID instrumentID) {
        instIDToUI.put(instrumentID, dmPicardUI);
    }

    public void addPicardRow(final PicardRowWithInstID row) {
        final PicardUI picardUI = instIDToUI.getOrDefault(row.instrumentID, defaultUI);
        picardUI.addPicardRow(row);
    }

    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {
        fiPicardUI.setDisplaySymbol(displaySymbol);
        dmPicardUI.setDisplaySymbol(displaySymbol);
    }

    public long flush() {
        final long delay1 = fiPicardUI.flush();
        final long delay2 = dmPicardUI.flush();
        return Math.min(delay1, delay2);
    }

}
