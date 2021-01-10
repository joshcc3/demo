package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByBestPrice;
import com.drwtrading.london.reddal.workingOrders.bestPrices.BestWorkingPriceForSymbol;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.photons.eeif.configuration.EeifConfiguration;
import com.drwtrading.photons.eeif.configuration.QuotingObligation;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;

import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FIETFObligationPresenter implements IWorkingOrdersCallback {

    private static final int MAX_PERCENT = 20;

    private static final long OBLIGATION_CALC_PERIOD_MILLIS = 1000;

    private final Map<String, WorkingOrdersByBestPrice> workingOrders;
    private final Map<String, FIETFObligationState> obligations;
    private final WebSocketViews<IFIETFObligationView> views;
    private final Set<String> failingObligations;

    private final IFuseBox<ReddalComponents> monitor;
    private final SelectIO uiSelectIO;
    private final boolean enabled;

    private final long minMilliSinceMidnight;
    private final long maxMilliSinceMidnight;
    private final long sysStartMillisSinceMidnight;

    public FIETFObligationPresenter(final boolean active, final SelectIO uiSelectIO, final IFuseBox<ReddalComponents> monitor) {

        // TODO: Add this field to all the prod config files
        this.enabled = active;

        this.uiSelectIO = uiSelectIO;
        this.monitor = monitor;

        this.workingOrders = new HashMap<>();
        this.obligations = new HashMap<>();

        this.views = new WebSocketViews<>(IFIETFObligationView.class, this);

        final Calendar cal = DateTimeUtil.getCalendar();
        cal.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        DateTimeUtil.setToTimeOfDay(cal, 8, 0, 0, 0);
        this.minMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        DateTimeUtil.setToTimeOfDay(cal, 16, 35, 0, 0);
        this.maxMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        this.sysStartMillisSinceMidnight = getMilliSinceMidnightNow();
        failingObligations = new HashSet<>();
    }

    public void start() {
        if (enabled) {
            uiSelectIO.addDelayedAction(OBLIGATION_CALC_PERIOD_MILLIS, this::calcObligations);
        }
    }

    public void setEeifConfig(final EeifConfiguration eeifConfig) {

        if (eeifConfig instanceof QuotingObligation) {

            final QuotingObligation quotingObligation = (QuotingObligation) eeifConfig;
            if (isXetraSymbol(quotingObligation.getSymbol()) && !obligations.containsKey(quotingObligation.getSymbol())) {
                obligations.put(quotingObligation.getSymbol(),
                        new FIETFObligationState(quotingObligation, uiSelectIO.getMillisSinceMidnightUTC(),
                                uiSelectIO.getMillisSinceMidnightUTC()));
            }
        }
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        if (isXetraSymbol(workingOrder.order.getSymbol())) {
            final WorkingOrdersByBestPrice orderedWorkingOrders =
                    workingOrders.computeIfAbsent(workingOrder.order.getSymbol(), WorkingOrdersByBestPrice::new);
            orderedWorkingOrders.setWorkingOrder(workingOrder);
        }
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder workingOrder) {

        if (isXetraSymbol(workingOrder.order.getSymbol())) {
            final WorkingOrdersByBestPrice orderedWorkingOrders = workingOrders.get(workingOrder.order.getSymbol());
            orderedWorkingOrders.removeWorkingOrder(workingOrder);
        }
    }

    @Override
    public void setNibblerDisconnected(final String source) {

        for (final WorkingOrdersByBestPrice orderedWorkingOrders : workingOrders.values()) {
            orderedWorkingOrders.connectionLost(source);
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketConnected) {

            onConnected((WebSocketConnected) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            onDisconnected((WebSocketDisconnected) webMsg);
        }
    }

    public void onConnected(final WebSocketConnected connected) {

        final IFIETFObligationView view = views.register(connected);
        for (final FIETFObligationState obligation : obligations.values()) {
            update(view, obligation);
        }
    }

    public void onDisconnected(final WebSocketDisconnected disconnected) {

        views.unregister(disconnected);
    }

    private long calcObligations() {

        for (final FIETFObligationState obligation : obligations.values()) {

            final WorkingOrdersByBestPrice workingOrders = this.workingOrders.get(obligation.getObligation().getSymbol());

            obligation.setState(uiSelectIO.getMillisSinceMidnightUTC(), isTwoSided(workingOrders));

            update(views.all(), obligation);
        }
        return OBLIGATION_CALC_PERIOD_MILLIS;
    }

    public void update(final IFIETFObligationView view, final FIETFObligationState obligationState) {

        final QuotingObligation obligation = obligationState.getObligation();

        final long percentage = getOnPercentage(obligationState);

        if (isFailing(percentage)) {
            if (!failingObligations.contains(obligation.getSymbol())) {
                failingObligations.add(obligation.getSymbol());
                monitor.logError(ReddalComponents.INVERSE_OBLIGATIONS, "Failing inverse obligation for " + obligation.getSymbol());
            }
        } else {
            monitor.setOK(ReddalComponents.INVERSE_OBLIGATIONS);
            failingObligations.remove(obligation.getSymbol());
        }

        view.setObligation(obligation.getSymbol(), isFailing(percentage), String.valueOf(percentage));
    }

    private static boolean isFailing(final double percentage) {
        return percentage >= MAX_PERCENT;
    }

    private static boolean isTwoSided(final WorkingOrdersByBestPrice workingOrders) {

        if (null == workingOrders) {
            return false;
        } else {
            final BestWorkingPriceForSymbol prices = workingOrders.getBestWorkingPrices();
            return prices.askPrice != null && prices.bidPrice != null;
        }
    }

    private static boolean isXetraSymbol(final String symbol) {
        return symbol.endsWith(MIC.XETR.getBBGCode(null));
    }

    private long getOnPercentage(final FIETFObligationState obligation) {
        final long millisTwoSided = obligation.getMillisTwoSided();
        final long totalTimeInTradingDay = this.maxMilliSinceMidnight - this.sysStartMillisSinceMidnight + obligation.getTotalTime();
        return (millisTwoSided * 2 * 100) / totalTimeInTradingDay;
    }

    private long getMilliSinceMidnightNow() {
        final long currentTimeMillis = uiSelectIO.getMillisSinceMidnightUTC();
        final long morningLimited = Math.max(currentTimeMillis, minMilliSinceMidnight);
        return Math.min(morningLimited, maxMilliSinceMidnight);
    }

}
