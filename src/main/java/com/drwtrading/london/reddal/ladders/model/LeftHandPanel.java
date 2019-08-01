package com.drwtrading.london.reddal.ladders.model;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.CSSClass;
import com.drwtrading.london.reddal.fastui.html.HTML;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.EnumMap;
import java.util.Map;

public class LeftHandPanel {

    private final UiPipeImpl ui;

    private final Map<QtyButton, Integer> qtyButtons;

    private final SimpleDateFormat timeSDF;
    private final DecimalFormat oneDP;
    private final DecimalFormat twoDP;
    private final DecimalFormat twoToTenDP;

    private long clockTime;

    private long deskPosition;
    private long lastTradeCOD;
    private long yesterdaySettlePrice;
    private long totalTradedQty;
    private long dayPosition;
    private long ourTradedQty;

    private double bpsTooltip;

    private double stackTickSize;
    private double stackTickToBPS;

    LeftHandPanel(final UiPipeImpl ui) {

        this.ui = ui;

        this.qtyButtons = new EnumMap<>(QtyButton.class);

        this.timeSDF = DateTimeUtil.getDateFormatter(DateTimeUtil.SIMPLE_TIME_FORMAT);
        this.timeSDF.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.oneDP = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 1);
        this.twoDP = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 2);
        this.twoToTenDP = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 2, 10);

        this.clockTime = 0;
        this.lastTradeCOD = Long.MIN_VALUE;
        this.yesterdaySettlePrice = Long.MIN_VALUE;
        this.totalTradedQty = Long.MIN_VALUE;
        this.dayPosition = Long.MIN_VALUE;
        this.ourTradedQty = Long.MIN_VALUE;

        this.bpsTooltip = 0d;

        this.stackTickSize = Double.MAX_VALUE;
        this.stackTickToBPS = Double.MAX_VALUE;
    }

    public void setTime(final long timeMillisUTC) {

        final long roundedToSecsMillisUTC = timeMillisUTC - (timeMillisUTC % 1000);
        if (clockTime != roundedToSecsMillisUTC) {
            this.clockTime = roundedToSecsMillisUTC;
            ui.txt(HTML.CLOCK, timeSDF.format(clockTime));
        }
    }

    public void setDeskPosition(final long position, final String formattedPosition) {

        if (this.deskPosition != position) {

            this.deskPosition = position;

            ui.txt(HTML.DESK_POSITION, formattedPosition);
            decorateUpDown(HTML.DESK_POSITION, position);
        }
    }

    public void setDayPosition(final long position, final String formattedPosition) {

        if (this.dayPosition != position) {

            this.dayPosition = position;

            ui.txt(HTML.POSITION, formattedPosition);
            decorateUpDown(HTML.POSITION, position);
        }
    }

    public void setOurTotalTradedQty(final long position, final String formattedQty) {

        if (this.ourTradedQty != position) {

            this.ourTradedQty = position;
            ui.txt(HTML.TOTAL_TRADED, formattedQty);
        }
    }

    public void setMktTotalTradedQty(final long totalTradedQty) {

        if (this.totalTradedQty != totalTradedQty) {

            this.totalTradedQty = totalTradedQty;

            final String formattedQty = formatVolume(totalTradedQty);
            ui.txt(HTML.TOTAL_TRADED_VOLUME, formattedQty);
        }
    }

    public void setLastTradeCOD(final Long lastTradeCOD, final MDForSymbol mdForSymbol) {

        if (this.lastTradeCOD != lastTradeCOD) {
            this.lastTradeCOD = lastTradeCOD;
            final String formattedPrice = mdForSymbol.formatPrice(lastTradeCOD);
            ui.txt(HTML.LAST_TRADE_COD, formattedPrice);
            decorateUpDown(HTML.LAST_TRADE_COD, lastTradeCOD);
        }
    }

    public void setYesterdaySettlePrice(final Long yesterdaySettlePrice, final MDForSymbol mdForSymbol) {

        if (this.yesterdaySettlePrice != yesterdaySettlePrice) {
            this.yesterdaySettlePrice = yesterdaySettlePrice;
            final String formattedPrice = mdForSymbol.formatPriceWithoutTrailingZeroes(yesterdaySettlePrice);
            ui.txt(HTML.YESTERDAY_SETTLE, formattedPrice);
        }
    }

    private void decorateUpDown(final String key, final long value) {

        ui.cls(key, CSSClass.POSITIVE, 0 < value);
        ui.cls(key, CSSClass.NEGATIVE, value < 0);
    }

    public void setBPSTooltip(final double bpsTooltip) {

        if (Constants.EPSILON < Math.abs(this.bpsTooltip - bpsTooltip)) {

            this.bpsTooltip = bpsTooltip;
            if (0 < bpsTooltip) {
                final String bps = twoDP.format(bpsTooltip) + "bps";
                ui.tooltip('#' + HTML.BUY_OFFSET_UP, bps);
                ui.tooltip('#' + HTML.SELL_OFFSET_UP, bps);
                ui.tooltip('#' + HTML.BUY_OFFSET_DOWN, '-' + bps);
                ui.tooltip('#' + HTML.SELL_OFFSET_DOWN, '-' + bps);
            } else {
                ui.tooltip('#' + HTML.BUY_OFFSET_UP, "");
                ui.tooltip('#' + HTML.SELL_OFFSET_UP, "");
                ui.tooltip('#' + HTML.BUY_OFFSET_DOWN, "");
                ui.tooltip('#' + HTML.SELL_OFFSET_DOWN, "");
            }
        }
    }

    public void setQtyButton(final QtyButton button, final int qty) {

        final Integer prevQty = qtyButtons.put(button, qty);
        if (null == prevQty || prevQty != qty) {
            final String formattedQty = formatClickQty(qty);
            ui.txt(button.htmlKey, formattedQty);
        }
    }

    private String formatClickQty(final Integer qty) {

        if (qty < 1_000) {
            return Integer.toString(qty);
        } else if (qty < 100_000) {
            return (qty / 1_000) + "K";
        } else {
            return oneDP.format(qty / 1_000_000d) + 'M';
        }
    }

    private String formatVolume(final long qty) {

        if (1_000_000 < qty) {
            return oneDP.format(qty / 1_000_000d) + 'M';
        } else if (1_000 < qty) {
            return oneDP.format(qty / 1_000d) + 'K';
        } else {
            return Long.toString(qty);
        }
    }

    public void setClickTradingQty(final long qty, final String fee) {
        ui.txt(HTML.INP_QTY, qty);
        ui.tooltip('#' + HTML.INP_QTY, fee);
    }

    public void setClickTradingPreference(final String prefKey, final String prefValue) {
        ui.txt(prefKey, prefValue);
    }

    public void setStackTickSize(final double tickSize) {

        if (Constants.EPSILON < Math.abs(tickSize - stackTickSize)) {

            this.stackTickSize = tickSize;
            final String formattedSize = twoToTenDP.format(tickSize);
            ui.txt(HTML.STACK_TICK_SIZE, formattedSize);
        }
    }

    public void setStackTickToBPS(final double tickToBPS) {

        if (Constants.EPSILON < Math.abs(tickToBPS - stackTickToBPS)) {

            this.stackTickToBPS = tickToBPS;
            final String formattedSize = twoToTenDP.format(tickToBPS);
            ui.txt(HTML.STACK_ALIGNMENT_TICK_TO_BPS, formattedSize);
        }
    }
}
