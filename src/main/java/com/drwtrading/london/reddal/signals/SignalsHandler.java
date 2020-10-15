package com.drwtrading.london.reddal.signals;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.picard.PicardRowState;
import com.drwtrading.london.reddal.picard.PicardSpotter;
import com.drwtrading.london.reddal.picard.PicardRow;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import drw.eeif.phockets.PhocketConnection;
import drw.eeif.phockets.PhocketHandler;
import drw.eeif.photons.signals.Alert;
import drw.eeif.photons.signals.AlertSignal;
import drw.eeif.photons.signals.AtCloseAlert;
import drw.eeif.photons.signals.ReactionSignal;
import drw.eeif.photons.signals.RestingOrderAlert;
import drw.eeif.photons.signals.Signals;
import drw.eeif.photons.signals.SweepAlert;
import drw.eeif.photons.signals.SymbolSideKey;
import drw.eeif.photons.signals.TheoSignal;
import drw.eeif.photons.signals.TwapAlert;
import drw.eeif.photons.signals.TweetAlert;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

public class SignalsHandler implements PhocketHandler<Signals, Void>, Alert.Visitor<Void> {

    private final Map<String, PicardRow> rowBySymbol = new HashMap<>();
    private final Publisher<PicardRow> rowPublisher;
    private final Publisher<StockAlert> stockAlerts;
    private final long milliAtMidnightUTC;
    private final DecimalFormat priceDF;
    private final SimpleDateFormat sdf;
    private final DecimalFormat thouDF;

    public SignalsHandler(final Publisher<PicardRow> rowPublisher, final Publisher<StockAlert> stockAlerts, final IClock clock) {
        this.rowPublisher = rowPublisher;
        this.stockAlerts = stockAlerts;
        this.milliAtMidnightUTC = clock.getMillisAtMidnightUTC();
        priceDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 5);
        sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);
        thouDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 1);

    }

    @Override
    public void connected(final PhocketConnection<Void> connection) {
        System.out.println("Signals connected" + connection.remote());
    }

    @Override
    public void closed(final PhocketConnection<Void> connection) {
        System.out.println("Signals disconnected: " + connection.remote());
    }

    @Override
    public void incoming(final PhocketConnection<Void> connection, final Signals value) {

        value.accept(new Signals.Visitor<Void>() {
            @Override
            public Void visitAlertSignal(final AlertSignal msg) {
                msg.getAlert().accept(SignalsHandler.this);
                return null;
            }

            @Override
            public Void visitReactionSignal(final ReactionSignal msg) {
                return null;
            }

            @Override
            public Void visitTheoSignal(final TheoSignal msg) {
                return null;
            }
        });
    }

    @Override
    public Void visitRestingOrderAlert(final RestingOrderAlert signal) {
        if (0 < signal.getPrice()) {
            final String timestamp = sdf.format(milliAtMidnightUTC + signal.getMillisSinceMidnight());
            final String start = sdf.format(signal.getTimePlacedMillisSinceMidnight());
            final String side = BookSide.BID == getSide(signal) ? "Buy " : "Sell ";
            final String price = priceDF.format(signal.getPrice() / (double) Constants.NORMALISING_FACTOR);
            final String msg = side + signal.getQty() + " @ " + price + " [at " + start + "].";
            final StockAlert alert = new StockAlert(signal.getMillisSinceMidnight(), timestamp, "RESTING_ORDER", signal.getSymbol(), msg);
            stockAlerts.publish(alert);
        }
        return null;
    }

    @Override
    public Void visitSweepAlert(final SweepAlert signal) {
        if (0 < signal.getNumLvls()) {
            final String timestamp = sdf.format(milliAtMidnightUTC + signal.getMillisSinceMidnight());
            final String action;
            if (BookSide.BID == getSide(signal)) {
                action = "Buys ";
            } else {
                action = "Sells ";
            }
            final String msg = action + signal.getNumLvls() + " levels [qty:" + signal.getQty() + "].";
            final StockAlert alert = new StockAlert(signal.getMillisSinceMidnight(), timestamp, "SWEEP", signal.getSymbol(), msg);
            stockAlerts.publish(alert);
        }
        return null;
    }

    @Override
    public Void visitTwapAlert(final TwapAlert signal) {

        if (0 < signal.volumeBucketMax) {
            final String timestamp = sdf.format(milliAtMidnightUTC + signal.getMillisSinceMidnight());
            final String period = thouDF.format(signal.twapPeriodMillis / 1000d);
            final String duration = thouDF.format(signal.twapDurationMillis / 1000d);
            final String action;
            if (BookSide.BID == getSide(signal)) {
                action = "Selling every ";
            } else {
                action = "Buying every ";
            }
            final String msg = action + period + " for " + duration + " seconds [Bucket " + signal.volumeBucketMin + ", " +
                    signal.volumeBucketMax + "].";

            final StockAlert alert = new StockAlert(signal.getMillisSinceMidnight(), timestamp, "TWAP", signal.getSymbol(), msg);
            stockAlerts.publish(alert);
        }
        return null;
    }

    @Override
    public Void visitTweetAlert(final TweetAlert signal) {
        if (!signal.getMsg().isEmpty()) {
            final String timestamp = sdf.format(milliAtMidnightUTC + signal.getMillisSinceMidnight());
            final String action = signal.getSide().name()+ ": ";
            final String msg = action + signal.getMsg();
            final StockAlert alert = new StockAlert(signal.getMillisSinceMidnight(), timestamp, "TWEET", signal.getSymbol(), msg);
            stockAlerts.publish(alert);
        }
        return null;
    }

    @Override
    public Void visitAtCloseAlert(final AtCloseAlert signal) {
        final String symbol = signal.getSymbol();
        final PicardRow oldRow = rowBySymbol.get(symbol);
        final boolean isNewRow = null == oldRow;
        if (0 < signal.getMillisSinceMidnight()) {
            final long signalMilliSinceUTC = milliAtMidnightUTC + signal.getMillisSinceMidnight();
            final String closePrice = priceDF.format(signal.closePrice / (double) Constants.NORMALISING_FACTOR);
            final BookSide bookSide = getSide(signal);
            if (null == bookSide) {
                return null;
            }
            final PicardRow row =
                    new PicardRow(signalMilliSinceUTC, signal.getSymbol(), InstType.EQUITY, null, bookSide.getOppositeSide(),
                            signal.closePrice, closePrice, PicardSpotter.getBPSThrough(signal.theoPrice, signal.closePrice),
                            Math.abs(signal.theoPrice - signal.closePrice), PicardRowState.LIVE, "AT_CLOSE", false, isNewRow);
            rowBySymbol.put(symbol, row);
            rowPublisher.publish(row);
        } else if (!isNewRow) {
            final PicardRow row = new PicardRow(oldRow, PicardRowState.DEAD);
            rowBySymbol.remove(symbol);
            rowPublisher.publish(row);
        }
        return null;
    }

    private BookSide getSide(final SymbolSideKey signal) {
        final BookSide bookSide;
        switch (signal.getSide()) {
            case BID:
                bookSide = BookSide.BID;
                break;
            case ASK:
                bookSide = BookSide.ASK;
                break;
            default:
                bookSide = null;
        }
        return bookSide;
    }


}
