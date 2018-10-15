package com.drwtrading.london.reddal.stockAlerts;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class StockAlertPresenter {

    private static final Set<String> UNWANTED_RFQ_FUTURES = new HashSet<>();
    private static final double RFQ_BIG_THRESHOLD = 10_000_000;

    static {
        UNWANTED_RFQ_FUTURES.add("FGBS");
        UNWANTED_RFQ_FUTURES.add("FGBM");
        UNWANTED_RFQ_FUTURES.add("FGBL");
        UNWANTED_RFQ_FUTURES.add("FGBX");
    }

    private static final long MAX_RFQ_ALERT_MILLIS = 60 * 1000;
    private static final int MAX_HISTORY = 15;

    private final IClock clock;
    private final UILogger webLog;
    private final FXCalc<?> fxCalc;

    private final DecimalFormat qtyDF;
    private final SimpleDateFormat sdf;
    private final WebSocketViews<IStockAlertsView> views;
    private final LinkedHashSet<StockAlert> alerts;
    private final long millisAtMidnightUTC = DateTimeUtil.getMillisAtMidnight();

    public StockAlertPresenter(final IClock clock, final FXCalc<?> fxCalc, final UILogger webLog) {

        this.clock = clock;
        this.fxCalc = fxCalc;
        this.webLog = webLog;

        this.qtyDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 0);
        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.views = WebSocketViews.create(IStockAlertsView.class, this);
        this.alerts = new LinkedHashSet<>();
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {

        final IStockAlertsView view = views.register(connected);
        for (final StockAlert update : alerts) {
            view.stockAlert(update.timestamp, update.type, update.symbol, update.msg, false);
        }
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        webLog.write("stockAlerts", msg);
    }

    public void addAlert(final StockAlert stockAlert) {
        if (alerts.add(stockAlert)) {

            final boolean isRecent = Math.abs(stockAlert.milliSinceMidnight - clock.getMillisSinceMidnightUTC()) < MAX_RFQ_ALERT_MILLIS;

            views.all().stockAlert(stockAlert.timestamp, stockAlert.type, stockAlert.symbol, stockAlert.msg, isRecent);

            if (MAX_HISTORY < alerts.size()) {
                final Iterator<?> it = alerts.iterator();
                it.next();
                it.remove();
            }
        }
    }

    private static boolean isRFQFiltered(final String symbol) {
        return 4 < symbol.length() && UNWANTED_RFQ_FUTURES.contains(symbol.substring(0, 4));
    }

    public void addRfq(final RfqAlert alert) {
        if (!isRFQFiltered(alert.symbol)) {

            final StockAlert stockAlert = getStockAlertFromRfq(alert);
            addAlert(stockAlert);
        }
    }

    StockAlert getStockAlertFromRfq(final RfqAlert alert) {
        final boolean validFx = fxCalc.isValid(alert.ccy, CCY.EUR);
        final double fxRate = validFx ? fxCalc.getMid(alert.ccy, CCY.EUR) : 1;
        final double value = alert.price * alert.qty * fxRate / Constants.NORMALISING_FACTOR;
        final String notional = qtyDF.format(value);

        final boolean bigRfq = alert.isETF && RFQ_BIG_THRESHOLD < value;
        final String type = (bigRfq ? "BIG_" : "") + (alert.isETF ? "ETF_RFQ" : "RFQ");

        return new StockAlert(alert.milliSinceMidnight, sdf.format(alert.milliSinceMidnight + millisAtMidnightUTC), type, alert.symbol,
                "Qty: " + qtyDF.format(alert.qty) + ", notional: " + notional + ' ' + (validFx ? CCY.EUR.name() : alert.ccy.name()));
    }
}
