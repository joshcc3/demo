package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.DataKey;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.ladders.PricingMode;

import java.text.DecimalFormat;
import java.util.ArrayList;

public class BookPanel {

    private static final int INITIAL_ROW_CAPACITY = 50;
    private static final int REALLY_BIG_NUMBER_THRESHOLD = 100_000;

    private final UiPipeImpl ui;

    private final ArrayList<BookPanelRow> rows;
    private final LongMap<BookPanelRow> rowsByPrice;

    private final DecimalFormat bpsDF;
    private final DecimalFormat efpDF;
    private final DecimalFormat bigNumberDF;

    private CCY ccy;
    private MIC mic;

    BookPanel(final UiPipeImpl ui) {

        this.ui = ui;

        this.rows = new ArrayList<>(INITIAL_ROW_CAPACITY);
        this.rowsByPrice = new LongMap<>();

        this.bpsDF = NumberFormatUtil.getDF(".0");
        this.efpDF = NumberFormatUtil.getDF("0.00");
        this.bigNumberDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE + 'M', 0, 2);

        clear();
    }

    void clear() {

        this.ccy = null;
        this.mic = null;

        rows.forEach(BookPanelRow::clear);
    }

    public void extendToLevels(final int levels) {

        for (int i = rows.size(); i < levels; ++i) {
            final BookPanelRow row = new BookPanelRow(i);
            rows.add(i, row);
        }
    }

    public boolean isEmpty() {
        return rows.isEmpty();
    }

    public BookPanelRow getRow(final int i) {
        return rows.get(i);
    }

    public void clearPriceMapping() {

        rowsByPrice.clear();
    }

    public void setRowPrice(final int rowID, final long price, final String formattedPrice) {

        final BookPanelRow row = rows.get(rowID);
        row.setPrice(price, formattedPrice);
        rowsByPrice.put(price, row);
    }

    public void sendLevelData(final int levels) {

        for (int i = 0; i < levels; ++i) {

            final BookPanelRow row = rows.get(i);
            ui.data(row.htmlData.bookBidKey, DataKey.PRICE, row.getPrice());
            ui.data(row.htmlData.bookAskKey, DataKey.PRICE, row.getPrice());
            ui.data(row.htmlData.bookOrderKey, DataKey.PRICE, row.getPrice());
        }
    }

    public BookPanelRow getRowByPrice(final long price) {
        return rowsByPrice.get(price);
    }

    public void setRawPrices(final int levels) {

        for (int i = 0; i < levels; ++i) {

            final BookPanelRow row = rows.get(i);
            if (row.setRawDisplayPrice()) {
                ui.txt(row.htmlData.bookPriceKey, row.getRawFormattedPrice());
            }
        }
    }

    public void setBPS(final BookPanelRow row, final double bps) {

        if (row.setDisplayPrice(PricingMode.BPS, bps)) {
            final String bpsText = bpsDF.format(bps);
            ui.txt(row.htmlData.bookPriceKey, bpsText);
        }
    }

    public void setEFP(final BookPanelRow row, final double efp) {

        if (row.setDisplayPrice(PricingMode.EFP, efp)) {
            final String efpText = efpDF.format(efp);
            ui.txt(row.htmlData.bookPriceKey, efpText);
        }
    }

    public void setCCY(final CCY ccy) {

        if (this.ccy != ccy && !rows.isEmpty()) {
            this.ccy = ccy;
            final BookPanelRow row = rows.get(0);
            ui.txt(row.htmlData.bookVolumeKey, ccy.name());
        }
    }

    public void setMIC(final MIC mic) {

        if (this.mic != mic && 1 < rows.size()) {
            this.mic = mic;
            final BookPanelRow row = rows.get(1);
            ui.txt(row.htmlData.bookVolumeKey, mic.name());
        }
    }

    public void setWorkingQty(final BookPanelRow row, final int qty) {

        if (row.setWorkingOrderQty(qty)) {
            final String formattedQty = formatMktQty(qty);
            ui.txt(row.htmlData.bookOrderKey, formattedQty);
        }
    }

    public void setBidQty(final BookPanelRow row, final long qty) {

        if (row.setBidQty(qty)) {
            final String formattedQty = formatMktQty(qty);
            ui.txt(row.htmlData.bookBidKey, formattedQty);
        }
    }

    public void setAskQty(final BookPanelRow row, final long qty) {

        if (row.setAskQty(qty)) {
            final String formattedQty = formatMktQty(qty);
            ui.txt(row.htmlData.bookAskKey, formattedQty);
        }
    }

    public void setVolume(final BookPanelRow row, final long volume) {

        if (1 < row.rowID && row.setVolume(volume)) {
            final String formattedQty = formatMktQty(volume);
            ui.txt(row.htmlData.bookVolumeKey, formattedQty);
        }
    }

    public void setLastTradePriceVolume(final BookPanelRow row, final long volume) {

        if (1 < row.rowID && row.setLastTradePriceVolume(volume)) {
            final String formattedQty = formatMktQty(volume);
            ui.txt(row.htmlData.bookTradeKey, formattedQty);
        }
    }

    private String formatMktQty(final long qty) {

        if (qty < 1) {
            return HTML.EMPTY;
        } else if (REALLY_BIG_NUMBER_THRESHOLD <= qty) {
            final double d = qty / 1_000_000d;
            return bigNumberDF.format(d);
        } else {
            return Long.toString(qty);
        }
    }
}
