package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByBestPrice;
import com.drwtrading.london.websocket.WebSocketViews;
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

        DateTimeUtil.setToTimeOfDay(cal, 16, 30, 0, 0);
        this.maxMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        this.sysStartMillisSinceMidnight = getMilliSinceMidnightNow();
        failingObligations = new HashSet<>();
    }

    public void start() {
        if (enabled) {
            uiSelectIO.addDelayedAction(OBLIGATION_CALC_PERIOD_MILLIS, this::calcObligations);
        }
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final String symbol = workingOrder.order.getSymbol();
        if (isXetraSymbol(symbol)) {
            final WorkingOrdersByBestPrice orderedWorkingOrders = workingOrders.computeIfAbsent(symbol, WorkingOrdersByBestPrice::new);
            orderedWorkingOrders.setWorkingOrder(workingOrder);

            final long currentMillisSinceMidnight = getMilliSinceMidnightNow();

            final FIETFObligationState obligation = obligations.computeIfAbsent(symbol,
                    orderSymbol -> new FIETFObligationState(orderSymbol, sysStartMillisSinceMidnight, currentMillisSinceMidnight,
                            maxMilliSinceMidnight));
            obligation.setState(currentMillisSinceMidnight, orderedWorkingOrders.isTwoSided());
        }
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final String symbol = workingOrder.order.getSymbol();
        if (isXetraSymbol(symbol)) {
            final WorkingOrdersByBestPrice orderedWorkingOrders = workingOrders.get(symbol);
            orderedWorkingOrders.removeWorkingOrder(workingOrder);

            final FIETFObligationState obligation = obligations.get(symbol);
            obligation.setState(getMilliSinceMidnightNow(), orderedWorkingOrders.isTwoSided());
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
            update(view, obligation, true);
        }
    }

    public void onDisconnected(final WebSocketDisconnected disconnected) {

        views.unregister(disconnected);
    }

    private long calcObligations() {

        final long nowMillisFromMidnight = getMilliSinceMidnightNow();

        for (final FIETFObligationState obligation : obligations.values()) {
            obligation.setState(nowMillisFromMidnight);
            update(views.all(), obligation, false);
        }
        return OBLIGATION_CALC_PERIOD_MILLIS;
    }

    public void update(final IFIETFObligationView view, final FIETFObligationState obligation, final boolean isNewConnection) {

        final int percentage = obligation.getTwoSidedPercentage();

        if (isFailing(percentage)) {
            if (!failingObligations.contains(obligation.getSymbol())) {
                failingObligations.add(obligation.getSymbol());
                monitor.logError(ReddalComponents.INVERSE_OBLIGATIONS, "Failing inverse obligation for " + obligation.getSymbol());
            }
        } else {
            failingObligations.remove(obligation.getSymbol());
        }

        if (failingObligations.isEmpty()) {
            monitor.setOK(ReddalComponents.INVERSE_OBLIGATIONS);
        }

        if (obligation.percentageChanged() || isNewConnection) {
            view.setObligation(obligation.getSymbol(), isFailing(percentage), String.valueOf(percentage));
        }
    }

    private static boolean isFailing(final double percentage) {
        return percentage >= MAX_PERCENT;
    }

    private static boolean isXetraSymbol(final String symbol) {
        return symbol.endsWith(MIC.XETR.getBBGCode(null));
    }

    private long getMilliSinceMidnightNow() {
        final long currentTimeMillis = uiSelectIO.getMillisSinceMidnightUTC();
        final long morningLimited = Math.max(currentTimeMillis, minMilliSinceMidnight);
        return Math.min(morningLimited, maxMilliSinceMidnight);
    }

}
