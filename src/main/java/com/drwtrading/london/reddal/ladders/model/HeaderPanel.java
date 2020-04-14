package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.indy.transport.data.Source;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.FreeTextCell;
import com.drwtrading.london.reddal.fastui.html.HTML;

import java.text.DecimalFormat;

public class HeaderPanel {

    private static final String RAW_AH_CELL_ID = HTML.TEXT_PREFIX + "r2c5";
    private static final String[] AH_LABELS;

    static {

        AH_LABELS = new String[102];

        for (int i = 0; i < AH_LABELS.length; ++i) {
            AH_LABELS[i] = Integer.toString(i);
        }
    }

    private final UiPipeImpl ui;

    private String title;

    private long bidQty;
    private String symbol;
    private String description;
    private long askQty;

    private long pksExposure;
    private long pksPosition;

    private int ahPercent;
    private int rawAHPercent;
    private double bestBidOffsetBPS;
    private double bestAskOffsetBPS;

    private final DecimalFormat twoDP;

    HeaderPanel(final UiPipeImpl ui) {

        this.ui = ui;

        this.title = null;
        this.bidQty = 0;
        this.symbol = null;
        this.description = null;
        this.askQty = 0;

        this.pksExposure = Long.MIN_VALUE;
        this.pksPosition = Long.MIN_VALUE;

        this.ahPercent = Integer.MIN_VALUE;
        this.rawAHPercent = Integer.MIN_VALUE;

        this.bestBidOffsetBPS = Double.NaN;
        this.bestAskOffsetBPS = Double.NaN;

        this.twoDP = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 2);
    }

    public void setTitle(final String title) {

        if (!title.equals(this.title)) {
            this.title = title;
            ui.title(title);
        }
    }

    public void setSymbol(final String symbol) {

        if (!symbol.equals(this.symbol)) {
            this.symbol = symbol;
            ui.txt(HTML.SYMBOL, symbol);
        }
    }

    public void setDescription(final String description) {

        if (!description.equals(this.description)) {
            this.description = description;
            ui.tooltip(HTML.SYMBOL, description);
        }
    }

    public void setBidQty(final long qty) {

        if (qty != bidQty) {
            this.bidQty = qty;
            ui.cls(HTML.BUY_QTY, CSSClass.INVISIBLE, 0 == qty);
            ui.txt(HTML.BUY_QTY, qty);
        }
    }

    public void setAskQty(final long qty) {

        if (qty != askQty) {
            this.askQty = qty;
            ui.cls(HTML.SELL_QTY, CSSClass.INVISIBLE, 0 == qty);
            ui.txt(HTML.SELL_QTY, qty);
        }
    }

    public void setPksExposure(final double exposure, final String formattedExposure) {

        final long roundedExposure = (long) exposure;
        if (this.pksExposure != roundedExposure) {

            this.pksExposure = roundedExposure;
            ui.txt(HTML.PKS_EXPOSURE, formattedExposure);
        }
    }

    public void setPksPosition(final double position, final String formattedPosition) {

        final long roundedPosition = (long) position;
        if (this.pksPosition != roundedPosition) {

            this.pksPosition = roundedPosition;
            ui.txt(HTML.PKS_POSITION, formattedPosition);
        }
    }

    public void setFreeText(final FreeTextCell cell, final String text) {
        ui.txt(cell.htmlID, text);
    }

    public void setCellDescription(final FreeTextCell cell, final String description) {
        ui.tooltip(cell.htmlID, description);
    }

    public void setTheoValue(final TheoValue theoValue) {

        if (null != theoValue) {

            final int ahPercent;
            final int rawAHPercent;

            if (theoValue.isValid()) {

                if (theoValue.getAfterHoursPct() < Constants.EPSILON) {
                    ahPercent = 0;
                } else {
                    ahPercent = (int) Math.ceil(theoValue.getAfterHoursPct());
                }
                rawAHPercent = (int) Math.ceil(theoValue.getRawAfterHoursPct());
            } else {
                ahPercent = -1;
                rawAHPercent = -1;
            }

            if (this.ahPercent != ahPercent) {

                this.ahPercent = ahPercent;
                setAHCell(HTML.AFTER_HOURS_WEIGHT, ahPercent);
            }

            ui.cls(HTML.AFTER_HOURS_WEIGHT, CSSClass.IS_POISONED, theoValue.isPoisoned());

            if (this.rawAHPercent != rawAHPercent) {
                this.rawAHPercent = rawAHPercent;
                setAHCell(RAW_AH_CELL_ID, rawAHPercent);
            }
        }
    }

    private void setAHCell(final String cellID, final int ahPercent) {

        if (ahPercent < 0) {
            ui.txt(cellID, "XXX");
        } else if (0 < ahPercent && ahPercent < AH_LABELS.length) {
            ui.txt(cellID, AH_LABELS[ahPercent]);
        } else {
            ui.txt(cellID, Integer.toString(ahPercent));
        }
    }

    public void setIndyData(final Source indyDefSource) {

        if (null != description) {
            ui.cls(HTML.AFTER_HOURS_WEIGHT, CSSClass.IS_ON_LINE_DEF, Source.ONLINE_VIEW == indyDefSource);
        }
    }

    public void setBestBidOffsetBPS(final double offsetBPS) {

        if (Double.isNaN(this.bestBidOffsetBPS) || Constants.EPSILON < Math.abs(this.bestBidOffsetBPS - offsetBPS)) {

            this.bestBidOffsetBPS = offsetBPS;
            setCellTest(HTML.BID_BEST_OFFSET_BPS, offsetBPS);
        }

    }

    public void setBestAskOffsetBPS(final double offsetBPS) {

        if (Double.isNaN(this.bestAskOffsetBPS) || Constants.EPSILON < Math.abs(this.bestAskOffsetBPS - offsetBPS)) {

            this.bestAskOffsetBPS = offsetBPS;
            setCellTest(HTML.ASK_BEST_OFFSET_BPS, offsetBPS);
        }
    }

    private void setCellTest(final String cellID, final double value) {

        if (Double.isNaN(value)) {
            ui.txt(cellID, "---");
        } else {
            final String formattedValue = twoDP.format(value);
            ui.txt(cellID, formattedValue);
        }
    }
}
