package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

class PicardDistanceTable {

    private static final int ROWS_TO_DISPLAY = 10;

    private final Map<String, String> uiSymbols;

    private final DecimalFormat bpsDF;

    private final NavigableSet<PicardDistanceData> allRows;

    private Map<String, PicardDistanceData> visibleRows;
    private Map<String, PicardDistanceData> workingRows;

    PicardDistanceTable() {

        this.uiSymbols = new HashMap<>();

        this.bpsDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 0, 2);

        this.allRows = new TreeSet<>();

        this.visibleRows = new HashMap<>();
        this.workingRows = new HashMap<>();
    }

    public void setDisplaySymbol(final String symbol, final String displaySymbol) {
        uiSymbols.put(symbol, displaySymbol);
    }

    void setData(final PicardDistanceData data) {
        allRows.add(data);
    }

    void removeData(final PicardDistanceData data) {
        allRows.remove(data);
    }

    void flushTo(final IPicardDistanceView view) {

        final Iterator<PicardDistanceData> orderedRows = allRows.iterator();

        boolean moreToShow = true;
        int displayedRows = 0;
        while (moreToShow && displayedRows < ROWS_TO_DISPLAY && orderedRows.hasNext()) {

            final PicardDistanceData data = orderedRows.next();

            if (data.isValid) {
                displayRow(view, data);
            } else {
                moreToShow = false;
            }

            ++displayedRows;
        }
    }

    void update(final IPicardDistanceView allViews) {

        final Iterator<PicardDistanceData> orderedRows = allRows.iterator();

        boolean moreToShow = true;
        int displayedRows = 0;
        while (moreToShow && displayedRows < ROWS_TO_DISPLAY && orderedRows.hasNext()) {

            final PicardDistanceData data = orderedRows.next();

            if (data.isValid) {

                workingRows.put(data.symbol, data);
                visibleRows.remove(data.symbol);

                displayRow(allViews, data);
            } else {
                moreToShow = false;
            }

            ++displayedRows;
        }

        for (final PicardDistanceData oldRow : visibleRows.values()) {
            allViews.remove(oldRow.side.name(), oldRow.symbol);
        }
        visibleRows.clear();

        final Map<String, PicardDistanceData> tmpRows = visibleRows;
        visibleRows = workingRows;
        workingRows = tmpRows;
    }

    private void displayRow(final IPicardDistanceView view, final PicardDistanceData data) {

        final String uiSymbol = getUISymbol(data.symbol);
        final String bpsAway = bpsDF.format(data.bpsFromTouch);

        view.set(data.side.name(), data.symbol, uiSymbol, bpsAway);
    }

    private String getUISymbol(final String symbol) {

        final String uiSymbol = uiSymbols.get(symbol);

        if (null == uiSymbol) {
            return symbol;
        } else {
            return uiSymbol;
        }
    }
}
