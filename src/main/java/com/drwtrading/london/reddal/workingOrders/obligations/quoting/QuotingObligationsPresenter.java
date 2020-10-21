package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
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

    private final SelectIO uiSelectIO;
    private final UILogger webLog;

    private final WebSocketViews<IQuotingObligationView> views;

    private final Map<String, NibblerClientHandler> nibblers;
    private final Map<String, QuotingObligationState> obligations;

    private final long minMilliSinceMidnight;
    private final long maxMilliSinceMidnight;
    private final long sysStartMillisSinceMidnight;

    private final String pageName;

    public QuotingObligationsPresenter(final StackCommunity community, final SelectIO uiSelectIO, final UILogger webLog) {

        this.uiSelectIO = uiSelectIO;
        this.webLog = webLog;

        this.views = WebSocketViews.create(IQuotingObligationView.class, this);

        this.nibblers = new HashMap<>();
        this.obligations = new HashMap<>();

        final Calendar cal = DateTimeUtil.getCalendar();
        cal.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        DateTimeUtil.setToTimeOfDay(cal, 8, 0, 0, 0);
        this.minMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        DateTimeUtil.setToTimeOfDay(cal, 16, 35, 0, 0);
        this.maxMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        this.sysStartMillisSinceMidnight = getNowMilliSinceMidnightNow();

        uiSelectIO.addDelayedAction(CHECK_OBLIGATION_PERIOD_MILLIS, this::checkObligations);

        pageName = "workingOrders#" + community;
    }

    public void setNibblerHandler(final String nibblerName, final NibblerClientHandler nibblerHandler) {
        nibblers.put(nibblerName, nibblerHandler);
    }

    private long getNowMilliSinceMidnightNow() {
        final long result = uiSelectIO.getMillisSinceMidnightUTC();
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
            result.setState(nowMilliSinceMidnight, quotingState.getStrategyID(), quotingState.isRunning(), quotingState.getStrategyInfo());
            result.setIsAvailable(true);
            return result;
        }
    }

    void onConnected(final WebSocketInboundData connected) {

        final IQuotingObligationView view = views.get(connected.getOutboundChannel());

        for (final QuotingObligationState state : obligations.values()) {
            setObligation(view, state);
        }
    }

    void onDisconnected(final WebSocketDisconnected disconnected) {

        views.unregister(disconnected);
    }

    public void onMessage(final WebSocketInboundData msg) {

        webLog.write(pageName, msg);
        views.invoke(msg);
    }

    @FromWebSocketView
    public void everythingOn(final WebSocketInboundData inboundData) {

        final User user = User.get(inboundData.getClient().getUserName());
        everythingOn(user);
    }

    public void enableQuotes(final QuoteObligationsEnableCmd cmd) {

        uiSelectIO.addDelayedAction(2_000, () -> everythingOn(cmd.user));
    }

    private long everythingOn(final User user) {

        final List<NibblerClientHandler> nibblers = new ArrayList<>();

        for (final QuotingObligationState quotingState : obligations.values()) {
            if (quotingState.isEnabled()) {
                final NibblerClientHandler nibblerClientHandler = quotingState.getNibblerClient();
                nibblers.add(nibblerClientHandler);
                nibblerClientHandler.startQuoter(quotingState.getStrategyID(), user);
            }

        }

        for (final NibblerClientHandler nibbler : nibblers) {
            nibbler.batchComplete();
        }

        return -1;
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
    public void startStrategy(final String symbol, final WebSocketInboundData inboundData) {

        final User user = User.get(inboundData.getClient().getUserName());
        startStrategy(symbol, user);
    }

    void startStrategy(final String symbol, final User user) {

        final QuotingObligationState quotingState = obligations.get(symbol);

        if (quotingState.isAvailable() && quotingState.isEnabled()) {
            final NibblerClientHandler nibblerClientHandler = quotingState.getNibblerClient();
            nibblerClientHandler.startQuoter(quotingState.getStrategyID(), user);
            nibblerClientHandler.batchComplete();
        }
    }

    @FromWebSocketView
    public void setEnabledState(final String symbol) {
        final QuotingObligationState quotingState = obligations.get(symbol);

        quotingState.setEnabled(!quotingState.isEnabled());

        if (!quotingState.isEnabled()) {
            if (quotingState.isStrategyOn()) {
                stopStrategy(symbol);
            }
        }

        setObligation(views.all(), quotingState);
    }

    @FromWebSocketView
    public void stopStrategy(final String symbol) {

        final QuotingObligationState quotingState = obligations.get(symbol);
        if (quotingState.isAvailable()) {
            final NibblerClientHandler nibblerClientHandler = quotingState.getNibblerClient();
            nibblerClientHandler.stopQuoter(quotingState.getStrategyID());
            nibblerClientHandler.batchComplete();
        }
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

        view.setRow(key, symbol, obligation.getSourceNibbler(), obligation.getOnPercent(), obligation.isEnabled(), isStrategyOn, isQuoting,
                obligation.getStateDescription(), isFailingObligation);
    }

    public void setNibblerDisconnected(final String sourceNibbler) {

        for (final QuotingObligationState obligation : obligations.values()) {

            if (obligation.getSourceNibbler().equals(sourceNibbler)) {
                obligation.setIsAvailable(false);
            }
        }
    }
}
