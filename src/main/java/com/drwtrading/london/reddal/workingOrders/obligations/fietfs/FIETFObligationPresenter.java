package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.obligations.quoting.IQuotingObligationView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

public final class FIETFObligationPresenter {

    private static final int CHECK_OBLIGATION_PERIOD_MILLIS = 30000;

    private static final int MAX_PERCENT = 20;

    private final IFuseBox<ReddalComponents> monitor;
    private final UILogger webLog;
    private final SelectIO uiSelectIO;

    private final EnumMap<StackCommunity, WebSocketViews<IQuotingObligationView>> communityViews;
    private final Map<Publisher<WebSocketOutboundData>, WebSocketViews<IQuotingObligationView>> userViews;

    private final Map<String, NibblerTransportOrderEntry> nibblers;

    private final Map<String, StackCommunity> symbolToCommunity;
    private final Map<String, FIETFObligationState> obligations;

    private final long minMilliSinceMidnight;
    private final long maxMilliSinceMidnight;
    private final long sysStartMillisSinceMidnight;
    private final Set<StackCommunity> primaryCommunities;

    public FIETFObligationPresenter(final Set<StackCommunity> primaryCommunities, final SelectIO uiSelectIO,
            final IFuseBox<ReddalComponents> monitor, final UILogger webLog) {
        this.primaryCommunities = primaryCommunities;

        this.uiSelectIO = uiSelectIO;
        this.monitor = monitor;
        this.webLog = webLog;

        this.userViews = new HashMap<>();
        this.communityViews = new EnumMap<>(StackCommunity.class);
        for (final StackCommunity primaryCommunity : StackCommunity.values()) {
            communityViews.put(primaryCommunity, WebSocketViews.create(IQuotingObligationView.class, this));
        }

        this.nibblers = new HashMap<>();
        this.obligations = new HashMap<>();
        this.symbolToCommunity = new HashMap<>();

        final Calendar cal = DateTimeUtil.getCalendar();
        cal.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        DateTimeUtil.setToTimeOfDay(cal, 8, 0, 0, 0);
        this.minMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        DateTimeUtil.setToTimeOfDay(cal, 16, 35, 0, 0);
        this.maxMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        this.sysStartMillisSinceMidnight = getMilliSinceMidnightNow();

        uiSelectIO.addDelayedAction(CHECK_OBLIGATION_PERIOD_MILLIS, this::checkObligations);

    }

    public void setNibblerHandler(final String nibblerName, final NibblerTransportOrderEntry nibblerHandler) {
        nibblers.put(nibblerName, nibblerHandler);
    }

    public void setSymbol(final StackCommunity community, final String symbol) {
        symbolToCommunity.put(symbol, community);
    }

    public void setQuotingState(final String sourceNibbler, final QuotingState quotingState) {

        final FIETFObligationState obligation = getObligation(sourceNibbler, quotingState);
        if (obligation.getSourceNibbler().equals(sourceNibbler)) {
            for (final Map.Entry<StackCommunity, WebSocketViews<IQuotingObligationView>> entry : communityViews.entrySet()) {
                setObligation(entry.getKey(), entry.getValue().all(), obligation);
            }
        }
    }

    void onSubscribe(final String communityStr, final WebSocketInboundData connected) {

        final StackCommunity uiCommunity = mapToCommunity(communityStr);
        if (null != uiCommunity && communityViews.containsKey(uiCommunity)) {

            final WebSocketViews<IQuotingObligationView> wsView = communityViews.get(uiCommunity);

            userViews.put(connected.getOutboundChannel(), wsView);

            final IQuotingObligationView view = wsView.get(connected.getOutboundChannel());
            doForSymbolsInUICommunity(uiCommunity, quotingState -> {
                setObligation(uiCommunity, view, quotingState);
                return null;
            });
        }
    }

    void onDisconnected(final WebSocketDisconnected disconnected) {
        userViews.get(disconnected.getOutboundChannel()).unregister(disconnected);
    }

    public void onMessage(final WebSocketInboundData msg) {

        webLog.write("inverseObligations", msg);

        final String[] cmdParts = msg.getData().split(",");
        if ("subscribeToCommunity".equals(cmdParts[0])) {
            onSubscribe(cmdParts[1], msg);
        } else {
            final WebSocketViews<IQuotingObligationView> view = userViews.get(msg.getOutboundChannel());
            if (null != view) {
                view.invoke(msg);
            }
        }
    }

    public void setNibblerDisconnected(final String sourceNibbler) {

        for (final FIETFObligationState obligation : obligations.values()) {

            if (obligation.getSourceNibbler().equals(sourceNibbler)) {
                obligation.setIsAvailable(false);
            }
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketDisconnected) {
            onDisconnected((WebSocketDisconnected) webMsg);
        } else if (webMsg instanceof WebSocketInboundData) {
            onMessage((WebSocketInboundData) webMsg);
        }
    }

    private long checkObligations() {

        final long nowMilliSinceMidnight = getMilliSinceMidnightNow();
        for (final FIETFObligationState obligation : obligations.values()) {

            obligation.updatePercent(nowMilliSinceMidnight);
            for (final Map.Entry<StackCommunity, WebSocketViews<IQuotingObligationView>> entry : communityViews.entrySet()) {
                setObligation(entry.getKey(), entry.getValue().all(), obligation);
                entry.getValue().all().checkWarning();
            }
            if (isFailingObligation(obligation)) {
                monitor.logError(ReddalComponents.INVERSE_OBLIGATIONS, "Two-sided quoting more than " + MAX_PERCENT + '%');
            } else {
                monitor.setOK(ReddalComponents.INVERSE_OBLIGATIONS);
            }
        }

        return CHECK_OBLIGATION_PERIOD_MILLIS;
    }

    private void setObligation(final StackCommunity community, final IQuotingObligationView view, final FIETFObligationState obligation) {

        final String symbol = obligation.getSymbol();

        if (StackCommunity.uiCommunityContains(community, symbolToCommunity.getOrDefault(symbol, StackCommunity.EXILES))) {
            final String key = obligation.getKey();
            final boolean isStrategyOn = obligation.isStrategyOn();
            final boolean isQuoting = obligation.isQuoting();
            final boolean isFailingObligation = obligation.getOnPercent() > MAX_PERCENT;

            view.setRow(key, symbol, obligation.getSourceNibbler(), obligation.getOnPercent(), obligation.isEnabled(), isStrategyOn,
                    isQuoting, obligation.getStateDescription(), isFailingObligation);
        }
    }

    private boolean isFailingObligation(final FIETFObligationState obligation) {
        final long totalTimeOn = obligation.getMillisOn();
        final long totalTimeInTradingDay = this.maxMilliSinceMidnight - this.sysStartMillisSinceMidnight + obligation.getTotalTime();
        return ((totalTimeOn * 2 * 100) / totalTimeInTradingDay) >= MAX_PERCENT;
    }

    private long getMilliSinceMidnightNow() {
        final long currentTimeMillis = uiSelectIO.getMillisSinceMidnightUTC();
        final long morningLimited = Math.max(currentTimeMillis, minMilliSinceMidnight);
        return Math.min(morningLimited, maxMilliSinceMidnight);
    }

    private FIETFObligationState getObligation(final String sourceNibbler, final QuotingState quotingState) {

        final String symbol = quotingState.getSymbol();
        final long nowMilliSinceMidnight = getMilliSinceMidnightNow();

        final FIETFObligationState result = obligations.get(symbol);
        if (null == result) {
            final NibblerTransportOrderEntry nibblerClient = nibblers.get(sourceNibbler);
            final FIETFObligationState newState =
                    new FIETFObligationState(symbol, quotingState.getStrategyID(), sourceNibbler, nibblerClient,
                            sysStartMillisSinceMidnight, nowMilliSinceMidnight, quotingState.isRunning(), quotingState.getStrategyInfo());
            obligations.put(symbol, newState);
            return newState;
        } else {
            result.setState(nowMilliSinceMidnight, quotingState.getStrategyID(), quotingState.isRunning(), quotingState.getStrategyInfo());
            result.setIsAvailable(true);
            return result;
        }
    }

    private void doForSymbolsInUICommunity(final StackCommunity uiCommunity, final Function<FIETFObligationState, Void> f) {
        for (final FIETFObligationState obligation : obligations.values()) {
            final String symbol = obligation.getSymbol();
            final StackCommunity symbolCommunity = symbolToCommunity.getOrDefault(symbol, StackCommunity.EXILES);
            if (StackCommunity.uiCommunityContains(uiCommunity, symbolCommunity)) {
                f.apply(obligation);
            }
        }
    }

    private StackCommunity mapToCommunity(final String communityStr) {
        final StackCommunity community = StackCommunity.get(communityStr);
        if (null == community && "DEFAULT".equals(communityStr)) {
            if (primaryCommunities.contains(StackCommunity.FUTURE)) {
                return StackCommunity.FUTURE;
            } else {
                return StackCommunity.DM;
            }
        } else {
            return community;
        }
    }

}
