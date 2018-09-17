package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.HTML;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class StackPanel {

    private static final int INITIAL_ROW_CAPACITY = 50;

    private final UiPipeImpl ui;

    private final ArrayList<StackPanelRow> rows;
    private final LongMap<StackPanelRow> rowsByPrice;

    private final DecimalFormat tickOffsetFormat;

    StackPanel(final UiPipeImpl ui) {

        this.ui = ui;

        this.rows = new ArrayList<>(INITIAL_ROW_CAPACITY);
        this.rowsByPrice = new LongMap<>();

        this.tickOffsetFormat = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 0);
    }

    public void extendToLevels(final int levels) {

        for (int i = rows.size(); i < levels; ++i) {
            final StackPanelRow row = new StackPanelRow(i);
            rows.add(i, row);
        }
    }

    void clear() {
        rows.forEach(StackPanelRow::clear);
    }

    public void clearPriceMapping() {

        rowsByPrice.clear();
    }

    public void setRowPrice(final int rowID, final long price) {

        final StackPanelRow row = rows.get(rowID);
        final String formattedPrice = tickOffsetFormat.format(price);
        row.setPrice(price, formattedPrice);
        rowsByPrice.put(price, row);
    }

    public StackPanelRow getRowByPrice(final long price) {
        return rowsByPrice.get(price);
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public StackPanelRow getRow(final int i) {
        return rows.get(i);
    }

    public void setBidQuoteQty(final StackPanelRow row, final long qty) {

        if (row.setBidQuoteQty(qty)) {
            ui.txt(row.htmlData.stackBidQuoteKey, formatQty(qty));
        }
    }

    public void setBidPicardQty(final StackPanelRow row, final long qty) {

        if (row.setBidPicardQty(qty)) {
            ui.txt(row.htmlData.stackBidPicardKey, formatQty(qty));
        }
    }

    public void setBidOffset(final StackPanelRow row, final String offset) {
        ui.txt(row.htmlData.stackBidOffsetKey, offset);
    }

    public void setAskOffset(final StackPanelRow row, final String offset) {
        ui.txt(row.htmlData.stackAskOffsetKey, offset);
    }

    public void setAskPicardQty(final StackPanelRow row, final long qty) {

        if (row.setAskPicardQty(qty)) {
            ui.txt(row.htmlData.stackAskPicardKey, formatQty(qty));
        }
    }

    public void setAskQuoteQty(final StackPanelRow row, final long qty) {

        if (row.setAskQuoteQty(qty)) {
            ui.txt(row.htmlData.stackAskQuoteKey, formatQty(qty));
        }
    }

    private static String formatQty(final long qty) {

        if (qty < 1) {
            return HTML.EMPTY;
        } else {
            return Long.toString(qty);
        }
    }
}
