package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;

public class QuotingObligationsPresenter {

    private static final int CHECK_OBLIGATION_PERIOD_MILLIS = 30000;

    private static final int MIN_PERCENT = 90;

    private final UILogger webLog;
    private final SelectIO uiSelectIO;

    private final EnumMap<StackCommunity, WebSocketViews<IQuotingObligationView>> communityViews;
    private final Map<Publisher<WebSocketOutboundData>, WebSocketViews<IQuotingObligationView>> userViews;

    private final Map<String, NibblerTransportOrderEntry> nibblers;

    private final Map<String, StackCommunity> symbolToCommunity;
    private final Map<String, QuotingObligationState> obligations;

    private final long minMilliSinceMidnight;
    private final long maxMilliSinceMidnight;
    private final long sysStartMillisSinceMidnight;
    private final Set<StackCommunity> primaryCommunities;

    public QuotingObligationsPresenter(final Set<StackCommunity> primaryCommunities, final SelectIO uiSelectIO, final UILogger webLog) {
        this.primaryCommunities = primaryCommunities;

        this.uiSelectIO = uiSelectIO;
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

        this.sysStartMillisSinceMidnight = getNowMilliSinceMidnightNow();

        uiSelectIO.addDelayedAction(CHECK_OBLIGATION_PERIOD_MILLIS, this::checkObligations);

    }

    public void setNibblerHandler(final String nibblerName, final NibblerTransportOrderEntry nibblerHandler) {
        nibblers.put(nibblerName, nibblerHandler);
    }

    public void setSymbol(final StackCommunity community, final String symbol) {
        symbolToCommunity.put(symbol, community);
    }

    public void setQuotingState(final String sourceNibbler, final QuotingState quotingState) {

        final QuotingObligationState obligation = getObligation(sourceNibbler, quotingState);
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

        webLog.write("quotingObligations", msg);

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

    @FromWebSocketView
    public void everythingOn(final String communityStr, final WebSocketInboundData inboundData) {

        final User user = User.get(inboundData.getClient().getUserName());
        final StackCommunity community = mapToCommunity(communityStr);
        if (null != community) {
            everythingOn(community, user);
        }

    }

    public void enableQuotes(final QuoteObligationsEnableCmd cmd) {
        uiSelectIO.addDelayedAction(2_000, () -> everythingOn(cmd.community, cmd.user));
    }

    @FromWebSocketView
    public void everythingOff(final String communityStr, final WebSocketInboundData inboundData) {

        final List<NibblerTransportOrderEntry> nibblers = new ArrayList<>();

        doForSymbolsInUICommunity(communityStr, quotingState -> {
            final NibblerTransportOrderEntry nibblerOE = quotingState.getNibblerOE();
            nibblers.add(nibblerOE);
            nibblerOE.stopQuoter(quotingState.getStrategyID());
            return null;
        });

        for (final NibblerTransportOrderEntry nibbler : nibblers) {
            nibbler.batchComplete();
        }
    }

    @FromWebSocketView
    public void startStrategy(final String communityStr, final String symbol, final WebSocketInboundData inboundData) {
        final StackCommunity c = mapToCommunity(communityStr);
        final User user = User.get(inboundData.getClient().getUserName());

        startStrategy(symbol, c, user);

    }

    @FromWebSocketView
    public void setEnabledState(final String communityStr, final String symbol, final WebSocketInboundData inboundData) {

        doForUICommunityAndSymbol(mapToCommunity(communityStr), symbol, (uiCommunity, symbolCommunity) -> {

            final QuotingObligationState quotingState = obligations.get(symbol);

            quotingState.setEnabled(!quotingState.isEnabled());

            if (!quotingState.isEnabled()) {
                if (quotingState.isStrategyOn()) {
                    stopStrategy(uiCommunity, symbol, inboundData);
                }
            }

            setObligation(uiCommunity, communityViews.get(uiCommunity).all(), quotingState);

            return null;
        });
    }

    @FromWebSocketView
    public void stopStrategy(final String communityStr, final String symbol, final WebSocketInboundData inboundData) {
        final StackCommunity uiCommunity = mapToCommunity(communityStr);
        stopStrategy(uiCommunity, symbol, inboundData);
    }

    public void setNibblerDisconnected(final String sourceNibbler) {

        for (final QuotingObligationState obligation : obligations.values()) {

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

    private void stopStrategy(final StackCommunity community, final String symbol, final WebSocketInboundData inboundData) {
        doForUICommunityAndSymbol(community, symbol, (uiCommunity, symbolCommunity) -> {
            final QuotingObligationState quotingState = obligations.get(symbol);
            if (quotingState.isAvailable()) {
                final NibblerTransportOrderEntry nibblerOE = quotingState.getNibblerOE();
                nibblerOE.stopQuoter(quotingState.getStrategyID());
                nibblerOE.batchComplete();
            }
            return null;
        });
    }

    private long checkObligations() {

        final long nowMilliSinceMidnight = getNowMilliSinceMidnightNow();
        for (final QuotingObligationState obligation : obligations.values()) {

            obligation.updatePercent(nowMilliSinceMidnight);
            for (final Map.Entry<StackCommunity, WebSocketViews<IQuotingObligationView>> entry : communityViews.entrySet()) {
                setObligation(entry.getKey(), entry.getValue().all(), obligation);
                entry.getValue().all().checkWarning();
            }
        }

        return CHECK_OBLIGATION_PERIOD_MILLIS;
    }

    private void setObligation(final StackCommunity community, final IQuotingObligationView view, final QuotingObligationState obligation) {

        final String symbol = obligation.getSymbol();

        if (StackCommunity.uiCommunityContains(community, symbolToCommunity.getOrDefault(symbol, StackCommunity.EXILES))) {
            final String key = obligation.getKey();
            final boolean isStrategyOn = obligation.isStrategyOn();
            final boolean isQuoting = obligation.isQuoting();
            final boolean isFailingObligation = obligation.getOnPercent() < MIN_PERCENT;

            view.setRow(key, symbol, obligation.getSourceNibbler(), obligation.getOnPercent(), obligation.isEnabled(), isStrategyOn,
                    isQuoting, obligation.getStateDescription(), isFailingObligation);
        }
    }

    private <T> T doForUICommunityAndSymbol(final StackCommunity uiCommunity, final String symbol,
            final BiFunction<StackCommunity, StackCommunity, T> f) {
        final StackCommunity symbolCommunity = symbolToCommunity.getOrDefault(symbol, StackCommunity.EXILES);
        if (null != uiCommunity && primaryCommunities.contains(uiCommunity) &&
                StackCommunity.uiCommunityContains(uiCommunity, symbolCommunity)) {
            return f.apply(uiCommunity, symbolCommunity);
        } else {
            return null;
        }
    }

    private long getNowMilliSinceMidnightNow() {
        final long result = uiSelectIO.getMillisSinceMidnightUTC();
        final long morningLimited = Math.max(result, minMilliSinceMidnight);
        return Math.min(morningLimited, maxMilliSinceMidnight);
    }

    private QuotingObligationState getObligation(final String sourceNibbler, final QuotingState quotingState) {

        final String symbol = quotingState.getSymbol();
        final long nowMilliSinceMidnight = getNowMilliSinceMidnightNow();

        final QuotingObligationState result = obligations.get(symbol);
        if (null == result) {
            final NibblerTransportOrderEntry nibblerClient = nibblers.get(sourceNibbler);
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

    private void doForSymbolsInUICommunity(final String communityStr, final Function<QuotingObligationState, Void> f) {
        final StackCommunity uiCommunity = mapToCommunity(communityStr);
        if (null != uiCommunity) {
            doForSymbolsInUICommunity(uiCommunity, f);
        }
    }

    private void doForSymbolsInUICommunity(final StackCommunity uiCommunity, final Function<QuotingObligationState, Void> f) {
        for (final QuotingObligationState obligation : obligations.values()) {
            final String symbol = obligation.getSymbol();
            final StackCommunity symbolCommunity = symbolToCommunity.getOrDefault(symbol, StackCommunity.EXILES);
            if (StackCommunity.uiCommunityContains(uiCommunity, symbolCommunity)) {
                f.apply(obligation);
            }
        }
    }

    private long everythingOn(final StackCommunity community, final User user) {

        final List<NibblerTransportOrderEntry> nibblers = new ArrayList<>();

        doForSymbolsInUICommunity(community, quotingState -> {
            if (quotingState.isEnabled()) {
                final NibblerTransportOrderEntry nibblerClientHandler = quotingState.getNibblerOE();
                nibblers.add(nibblerClientHandler);
                nibblerClientHandler.startQuoter(quotingState.getStrategyID(), user);
            }
            return null;
        });

        for (final NibblerTransportOrderEntry nibbler : nibblers) {
            nibbler.batchComplete();
        }

        return -1;

    }

    private void startStrategy(final String symbol, final StackCommunity c, final User user) {
        doForUICommunityAndSymbol(c, symbol, (uiCommunity, symbolCommunity) -> {
            startStrategy(symbolCommunity, symbol, user);
            return null;
        });
    }

    void startStrategy(final StackCommunity community, final String symbol, final User user) {

        doForUICommunityAndSymbol(community, symbol, (uiCommunity, symbolCommunity) -> {
            final QuotingObligationState quotingState = obligations.get(symbol);

            if (quotingState.isAvailable() && quotingState.isEnabled()) {
                final NibblerTransportOrderEntry nibblerOE = quotingState.getNibblerOE();
                nibblerOE.startQuoter(quotingState.getStrategyID(), user);
                nibblerOE.batchComplete();
            }
            return null;
        });
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
