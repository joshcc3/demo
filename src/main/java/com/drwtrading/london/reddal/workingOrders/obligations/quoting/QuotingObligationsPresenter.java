package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class QuotingObligationsPresenter {

    private static final int CHECK_OBLIGATION_PERIOD_MILLIS = 30000;

    private static final int MIN_PERCENT = 90;

    private final IClock clock;

    private final UILogger webLog;
    private final FiberBuilder logFiber;

    private final WebSocketViews<IQuotingObligationView> views;

    private final long minMilliSinceMidnight;
    private final long maxMilliSinceMidnight;
    private final long sysStartMillisSinceMidnight;

    private final Map<String, QuotingObligationState> obligations;

    public QuotingObligationsPresenter(final SelectIO uiSelectIO, final UILogger webLog, final FiberBuilder logFiber) {

        this.clock = uiSelectIO;

        this.webLog = webLog;
        this.logFiber = logFiber;

        this.views = WebSocketViews.create(IQuotingObligationView.class, this);

        this.obligations = new HashMap<>();

        final Calendar cal = DateTimeUtil.getCalendar();
        cal.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        DateTimeUtil.setToTimeOfDay(cal, 8, 0, 0, 0);
        this.minMilliSinceMidnight = cal.getTimeInMillis() - clock.getMillisAtMidnightUTC();

        DateTimeUtil.setToTimeOfDay(cal, 16, 35, 0, 0);
        this.maxMilliSinceMidnight = cal.getTimeInMillis() - clock.getMillisAtMidnightUTC();

        this.sysStartMillisSinceMidnight = getNowMilliSinceMidnightNow();

        uiSelectIO.addDelayedAction(CHECK_OBLIGATION_PERIOD_MILLIS, this::checkObligations);
    }

    private long getNowMilliSinceMidnightNow() {
        final long result = clock.getMillisSinceMidnightUTC();
        final long morningLimited = Math.max(result, minMilliSinceMidnight);
        return Math.min(morningLimited, maxMilliSinceMidnight);
    }

    public void setStrategyState(final QuotingState state) {

        final QuotingObligationState obligation = getObligation(state.getSymbol(), state.isRunning());
        setObligation(views.all(), obligation);
    }

    private QuotingObligationState getObligation(final String symbol, final boolean isOn) {

        final long nowMilliSinceMidnight = getNowMilliSinceMidnightNow();
        final QuotingObligationState result = obligations.get(symbol);
        if (null == result) {
            final QuotingObligationState newState =
                    new QuotingObligationState(symbol, sysStartMillisSinceMidnight, nowMilliSinceMidnight, isOn);
            obligations.put(symbol, newState);
            return newState;
        } else {
            result.setState(nowMilliSinceMidnight, isOn);
            return result;
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketConnected) {

            onConnected((WebSocketConnected) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            onDisconnected((WebSocketDisconnected) webMsg);

        }
    }

    private void onConnected(final WebSocketConnected connected) {

        final IQuotingObligationView view = views.register(connected);

        for (final QuotingObligationState state : obligations.values()) {
            setObligation(view, state);
        }
    }

    private void onDisconnected(final WebSocketDisconnected disconnected) {

        views.unregister(disconnected);
    }

    private long checkObligations() {

        final long nowMilliSinceMidnight = getNowMilliSinceMidnightNow();
        for (final QuotingObligationState obligation : obligations.values()) {

            obligation.updatePercent(nowMilliSinceMidnight);
            setObligation(views.all(), obligation);
        }

        views.all().checkWarning();
        return CHECK_OBLIGATION_PERIOD_MILLIS;
    }

    private static void setObligation(final IQuotingObligationView view, final QuotingObligationState obligation) {

        final String symbol = obligation.getSymbol();
        final String key = obligation.getKey();
        final boolean isOn = obligation.isOn();
        final boolean isFailingObligation = obligation.getOnPercent() < MIN_PERCENT;

        view.setRow(key, symbol, obligation.getOnPercent(), isOn, isFailingObligation);
    }
}
