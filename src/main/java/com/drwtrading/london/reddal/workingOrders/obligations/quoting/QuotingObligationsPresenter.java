package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QuotingObligationsPresenter {

    private static final int CHECK_OBLIGATION_PERIOD_MILLIS = 30000;

    private static final int MIN_PERCENT = 90;

    private final IClock clock;
    private final UILogger webLog;

    private final WebSocketViews<IQuotingObligationView> views;

    private final Map<String, NibblerClientHandler> nibblers;
    private final Map<String, QuotingObligationState> obligations;

    private final long minMilliSinceMidnight;
    private final long maxMilliSinceMidnight;
    private final long sysStartMillisSinceMidnight;

    public QuotingObligationsPresenter(final SelectIO uiSelectIO, final UILogger webLog) {

        this.clock = uiSelectIO;
        this.webLog = webLog;

        this.views = WebSocketViews.create(IQuotingObligationView.class, this);

        this.nibblers = new HashMap<>();
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

    public void setNibblerHandler(final String nibblerName, final NibblerClientHandler nibblerHandler) {

        nibblers.put(nibblerName, nibblerHandler);
    }

    private long getNowMilliSinceMidnightNow() {
        final long result = clock.getMillisSinceMidnightUTC();
        final long morningLimited = Math.max(result, minMilliSinceMidnight);
        return Math.min(morningLimited, maxMilliSinceMidnight);
    }

    public void setQuotingState(final String sourceNibbler, final QuotingState quotingState) {

        final QuotingObligationState obligation = getObligation(sourceNibbler, quotingState);
        if (obligation.getSourceNibbler().equals(sourceNibbler)) {
            setObligation(views.all(), obligation);
        }
    }

    private QuotingObligationState getObligation(final String sourceNibbler, final QuotingState quotingState) {

        final String symbol = quotingState.getSymbol();
        final long nowMilliSinceMidnight = getNowMilliSinceMidnightNow();
        final QuotingObligationState result = obligations.get(symbol);
        if (null == result) {
            final NibblerClientHandler nibblerClient = nibblers.get(sourceNibbler);
            final QuotingObligationState newState =
                    new QuotingObligationState(symbol, quotingState.getStrategyID(), sourceNibbler, nibblerClient,
                            sysStartMillisSinceMidnight, nowMilliSinceMidnight, quotingState.isRunning(), quotingState.getStrategyInfo());
            obligations.put(symbol, newState);
            return newState;
        } else {
            result.setState(nowMilliSinceMidnight, quotingState.isRunning(), quotingState.getStrategyInfo());
            return result;
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketConnected) {

            onConnected((WebSocketConnected) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            onDisconnected((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            onMessage((WebSocketInboundData) webMsg);
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

    public void onMessage(final WebSocketInboundData msg) {

        webLog.write("workingOrders", msg);
        views.invoke(msg);
    }

    @FromWebSocketView
    public void everythingOn(final WebSocketInboundData inboundData) {

        everythingOn();
    }

    public void enableQuotes(final QuoteObligationsEnableCmd quoteObligationsEnableCmd) {

        everythingOn();
    }

    private void everythingOn() {

        final List<NibblerClientHandler> nibblers = new ArrayList<>();

        for (final QuotingObligationState quotingState : obligations.values()) {

            final NibblerClientHandler nibblerClientHandler = quotingState.getNibblerClient();
            nibblers.add(nibblerClientHandler);
            nibblerClientHandler.startQuoter(quotingState.getStrategyID());
        }

        for (final NibblerClientHandler nibbler : nibblers) {
            nibbler.batchComplete();
        }
    }

    @FromWebSocketView
    public void everythingOff(final WebSocketInboundData inboundData) {

        final List<NibblerClientHandler> nibblers = new ArrayList<>();

        for (final QuotingObligationState quotingState : obligations.values()) {

            final NibblerClientHandler nibblerClientHandler = quotingState.getNibblerClient();
            nibblers.add(nibblerClientHandler);
            nibblerClientHandler.stopQuoter(quotingState.getStrategyID());
        }

        for (final NibblerClientHandler nibbler : nibblers) {
            nibbler.batchComplete();
        }
    }

    @FromWebSocketView
    public void startStrategy(final String symbol) {

        final QuotingObligationState quotingState = obligations.get(symbol);
        final NibblerClientHandler nibblerClientHandler = quotingState.getNibblerClient();
        nibblerClientHandler.startQuoter(quotingState.getStrategyID());
        nibblerClientHandler.batchComplete();
    }

    @FromWebSocketView
    public void stopStrategy(final String symbol) {

        final QuotingObligationState quotingState = obligations.get(symbol);
        final NibblerClientHandler nibblerClientHandler = quotingState.getNibblerClient();
        nibblerClientHandler.stopQuoter(quotingState.getStrategyID());
        nibblerClientHandler.batchComplete();
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
        final boolean isStrategyOn = obligation.isStrategyOn();
        final boolean isQuoting = obligation.isQuoting();
        final boolean isFailingObligation = obligation.getOnPercent() < MIN_PERCENT;

        view.setRow(key, symbol, obligation.getSourceNibbler(), obligation.getOnPercent(), isStrategyOn, isQuoting,
                obligation.getStateDescription(), isFailingObligation);
    }
}
