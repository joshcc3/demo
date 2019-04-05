package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

class LiquidityFinderViewTable {

    private static final int ROWS_TO_DISPLAY = 10;

    private final Map<String, String> uiSymbols;

    private final DecimalFormat bpsDF;

    private final NavigableSet<LiquidityFinderData> allRows;

    private Map<String, LiquidityFinderData> visibleRows;
    private Map<String, LiquidityFinderData> workingRows;

    LiquidityFinderViewTable() {

        this.uiSymbols = new HashMap<>();

        this.bpsDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 0, 2);

        this.allRows = new TreeSet<>();

        this.visibleRows = new HashMap<>();
        this.workingRows = new HashMap<>();
    }

    public void setDisplaySymbol(final String symbol, final String displaySymbol) {
        uiSymbols.put(symbol, displaySymbol);
    }

    void setData(final LiquidityFinderData data) {
        allRows.add(data);
    }

    void removeData(final LiquidityFinderData data) {
        allRows.remove(data);
    }

    void flushTo(final ILiquidityFinderView view) {

        final Iterator<LiquidityFinderData> orderedRows = allRows.iterator();

        boolean moreToShow = true;
        int displayedRows = 0;
        while (moreToShow && displayedRows < ROWS_TO_DISPLAY && orderedRows.hasNext()) {

            final LiquidityFinderData data = orderedRows.next();

            if (data.isValid) {
                displayRow(view, data);
            } else {
                moreToShow = false;
            }

            ++displayedRows;
        }
    }

    void update(final ILiquidityFinderView allViews) {

        final Iterator<LiquidityFinderData> orderedRows = allRows.iterator();

        boolean moreToShow = true;
        int displayedRows = 0;
        while (moreToShow && displayedRows < ROWS_TO_DISPLAY && orderedRows.hasNext()) {

            final LiquidityFinderData data = orderedRows.next();

            if (data.isValid) {

                workingRows.put(data.symbol, data);
                visibleRows.remove(data.symbol);

                displayRow(allViews, data);
            } else {
                moreToShow = false;
            }

            ++displayedRows;
        }

        for (final LiquidityFinderData oldRow : visibleRows.values()) {
            allViews.remove(oldRow.side.name(), oldRow.symbol);
        }
        visibleRows.clear();

        final Map<String, LiquidityFinderData> tmpRows = visibleRows;
        visibleRows = workingRows;
        workingRows = tmpRows;
    }

    private void displayRow(final ILiquidityFinderView view, final LiquidityFinderData data) {

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
