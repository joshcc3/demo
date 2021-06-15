package com.drwtrading.london.reddal.stockAlerts;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
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
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class StockAlertPresenter {

    private static final Set<String> UNWANTED_RFQ_FUTURES = new HashSet<>();

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
    private final EnumMap<StackCommunity, WebSocketViews<IStockAlertsView>> communityViews;
    private final EnumMap<StackCommunity, LinkedHashSet<StockAlert>> communityAlerts;
    private final Set<RfqAlert> seenRFQs;
    private final long millisAtMidnightUTC = DateTimeUtil.getMillisAtMidnight();
    private final Map<String, StackCommunity> symbolCommunity;

    public StockAlertPresenter(final IClock clock, final FXCalc<?> fxCalc, final UILogger webLog) {

        this.clock = clock;
        this.fxCalc = fxCalc;
        this.webLog = webLog;

        this.qtyDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 0);
        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);
        this.sdf.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        this.seenRFQs = new HashSet<>();

        this.communityViews = new EnumMap<>(StackCommunity.class);
        final WebSocketViews<IStockAlertsView> dmView = WebSocketViews.create(IStockAlertsView.class, this);
        final WebSocketViews<IStockAlertsView> fiView = WebSocketViews.create(IStockAlertsView.class, this);
        final WebSocketViews<IStockAlertsView> fcView = WebSocketViews.create(IStockAlertsView.class, this);
        final WebSocketViews<IStockAlertsView> emView = WebSocketViews.create(IStockAlertsView.class, this);
        this.communityAlerts = new EnumMap<>(StackCommunity.class);
        final LinkedHashSet<StockAlert> dmAlerts = new LinkedHashSet<>();
        final LinkedHashSet<StockAlert> fiAlerts = new LinkedHashSet<>();
        final LinkedHashSet<StockAlert> fcAlerts = new LinkedHashSet<>();
        final LinkedHashSet<StockAlert> emAlerts = new LinkedHashSet<>();

        for (final StackCommunity community : StackCommunity.values()) {

            switch (community) {
                case FI: {
                    this.communityViews.put(community, fiView);
                    this.communityAlerts.put(community, fiAlerts);
                    break;
                }
                case EM: {
                    this.communityViews.put(community, emView);
                    this.communityAlerts.put(community, emAlerts);
                    break;
                }
                case FC: {
                    this.communityViews.put(community, fcView);
                    this.communityAlerts.put(community, fcAlerts);
                    break;
                }
                case DM:
                default: {
                    this.communityViews.put(community, dmView);
                    this.communityAlerts.put(community, dmAlerts);
                }
            }
        }

        symbolCommunity = new HashMap<>();
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        // no-op
    }

    public void setCommunityForSymbol(final String symbol, final StackCommunity community) {
        symbolCommunity.put(symbol, community);
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {

        for (final WebSocketViews<IStockAlertsView> view : communityViews.values()) {
            view.unregister(disconnected);
        }
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {

        webLog.write("stockAlerts", msg);
        final String data = msg.getData();

        final String[] cmdParts = data.split(",");
        if ("subscribeToCommunity".equals(cmdParts[0])) {
            subscribeToCommunity(cmdParts[1], msg);
        }
    }

    public void subscribeToCommunity(final String communityStr, final WebSocketInboundData data) {

        final StackCommunity community = StackCommunity.get(communityStr.toUpperCase());
        if (null != community) {

            final IStockAlertsView newView = communityViews.get(community).get(data.getOutboundChannel());
            final LinkedHashSet<StockAlert> stockAlerts = communityAlerts.get(community);

            for (final StockAlert update : stockAlerts) {

                newView.stockAlert(update.timestamp, update.type, update.symbol, update.msg, false);
            }
        }
    }

    public void addRfq(final RfqAlert alert) {

        final String symbol = alert.symbol;
        final boolean isRFQWanted = symbol.length() < 4 || !UNWANTED_RFQ_FUTURES.contains(symbol.substring(0, 4));

        if (isRFQWanted && seenRFQs.add(alert)) {

            final StockAlert stockAlert = getStockAlertFromRfq(alert);
            addAlert(stockAlert);
        }
    }

    private StockAlert getStockAlertFromRfq(final RfqAlert alert) {

        final boolean validFx = fxCalc.isValid(alert.ccy, CCY.EUR);
        final double fxRate = validFx ? fxCalc.getMid(alert.ccy, CCY.EUR) : 1;
        final long qty = alert.isValidQty() ? alert.qty : 0;
        final double value = alert.price * qty * fxRate / Constants.NORMALISING_FACTOR;
        final String notional = qtyDF.format(value);

        final String msg = "Qty: " + qtyDF.format(qty) + ", notional: " + notional + ' ' + (validFx ? CCY.EUR.name() : alert.ccy.name());
        return new StockAlert(alert.milliSinceMidnight, sdf.format(alert.milliSinceMidnight + millisAtMidnightUTC), "RFQ", alert.symbol,
                msg);
    }

    public void addAlert(final StockAlert stockAlert) {

        final StackCommunity community = symbolCommunity.getOrDefault(stockAlert.symbol, StackCommunity.DM);
        final LinkedHashSet<StockAlert> stockAlerts = communityAlerts.get(community);
        if (!stockAlerts.contains(stockAlert)) {

            stockAlerts.add(stockAlert);
            final boolean isRecent = Math.abs(stockAlert.milliSinceMidnight - clock.getMillisSinceMidnightUTC()) < MAX_RFQ_ALERT_MILLIS;

            final WebSocketViews<IStockAlertsView> stockAlertView = communityViews.get(community);

            stockAlertView.all().stockAlert(stockAlert.timestamp, stockAlert.type, stockAlert.symbol, stockAlert.msg, isRecent);

            if (MAX_HISTORY < stockAlerts.size()) {
                final Iterator<?> it = stockAlerts.iterator();
                it.next();
                it.remove();
            }
        }
    }

}
