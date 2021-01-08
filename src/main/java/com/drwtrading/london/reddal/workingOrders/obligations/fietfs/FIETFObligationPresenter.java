package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class FIETFObligationPresenter {

    private static final int CHECK_OBLIGATION_PERIOD_MILLIS = 30000;

    private static final int MAX_PERCENT = 20;

    private static final String GM = "gm";
    private static final String XETRA = "xetra";

    private final IFuseBox<ReddalComponents> monitor;
    private final UILogger webLog;
    private final SelectIO uiSelectIO;
    private final boolean enabled;

    private final WebSocketViews<IQuotingObligationView> fiCommunityView;
    private final Map<Publisher<WebSocketOutboundData>, WebSocketViews<IQuotingObligationView>> userViews;

    private final Map<String, NibblerTransportOrderEntry> nibblers;

    private final Map<String, FIETFObligationState> obligations;

    private final long minMilliSinceMidnight;
    private final long maxMilliSinceMidnight;
    private final long sysStartMillisSinceMidnight;
    private final Map<String, HashSet<String>> failingObligations;

    public FIETFObligationPresenter(final String appName, final SelectIO uiSelectIO, final IFuseBox<ReddalComponents> monitor,
            final UILogger webLog) {

        this.enabled = appName.toLowerCase().contains(GM);

        this.uiSelectIO = uiSelectIO;
        this.monitor = monitor;
        this.webLog = webLog;

        this.userViews = new HashMap<>();
        this.fiCommunityView = WebSocketViews.create(IQuotingObligationView.class, this);

        this.nibblers = new HashMap<>();
        this.obligations = new HashMap<>();

        final Calendar cal = DateTimeUtil.getCalendar();
        cal.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        DateTimeUtil.setToTimeOfDay(cal, 8, 0, 0, 0);
        this.minMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        DateTimeUtil.setToTimeOfDay(cal, 16, 35, 0, 0);
        this.maxMilliSinceMidnight = cal.getTimeInMillis() - uiSelectIO.getMillisAtMidnightUTC();

        this.sysStartMillisSinceMidnight = getMilliSinceMidnightNow();

        uiSelectIO.addDelayedAction(CHECK_OBLIGATION_PERIOD_MILLIS, this::checkObligations);

        failingObligations = new HashMap<>();
    }

    public void setNibblerHandler(final String nibblerName, final NibblerTransportOrderEntry nibblerHandler) {

        if (enabled && isXetraNibbler(nibblerName)) {
            nibblers.put(nibblerName, nibblerHandler);
        }
    }

    private static boolean isXetraNibbler(final String nibblerName) {
        return nibblerName.toLowerCase().contains(XETRA);
    }

    public void setQuotingState(final String sourceNibbler, final QuotingState quotingState) {

        if (enabled && isXetraNibbler(sourceNibbler)) {
            final FIETFObligationState obligation = getObligation(sourceNibbler, quotingState);
            if (obligation.getSourceNibbler().equals(sourceNibbler)) {
                setObligation(StackCommunity.FI, fiCommunityView.all(), obligation);
            }
        }
    }

    void onSubscribe(final String communityStr, final WebSocketInboundData connected) {

        final StackCommunity uiCommunity = mapToCommunity(communityStr);
        if (uiCommunity == StackCommunity.FI) {

            userViews.put(connected.getOutboundChannel(), fiCommunityView);

            final IQuotingObligationView view = fiCommunityView.get(connected.getOutboundChannel());
            for (final FIETFObligationState obligation : obligations.values()) {
                setObligation(uiCommunity, view, obligation);
            }
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

        if (enabled && isXetraNibbler(sourceNibbler)) {

            for (final FIETFObligationState obligation : obligations.values()) {
                if (obligation.getSourceNibbler().equals(sourceNibbler)) {
                    obligation.setIsAvailable(false);
                }
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
            setObligation(StackCommunity.FI, fiCommunityView.all(), obligation);
            fiCommunityView.all().checkWarning();

            final Set<String> failingSymbols = MapUtils.getMappedSet(failingObligations, obligation.getSourceNibbler());
            if (isFailingObligation(obligation)) {
                if (!failingSymbols.contains(obligation.getSymbol())) {
                    failingSymbols.add(obligation.getSymbol());
                    monitor.logError(ReddalComponents.INVERSE_OBLIGATIONS, "Failing inverse obligation for " + obligation.getSymbol());
                }
            } else {
                monitor.setOK(ReddalComponents.INVERSE_OBLIGATIONS);
                failingSymbols.remove(obligation.getSymbol());
            }
        }

        return CHECK_OBLIGATION_PERIOD_MILLIS;
    }

    private static void setObligation(final StackCommunity community, final IQuotingObligationView view,
            final FIETFObligationState obligation) {

        final String symbol = obligation.getSymbol();

        if (StackCommunity.FI == community) {
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

    private static StackCommunity mapToCommunity(final String communityStr) {
        final StackCommunity community = StackCommunity.get(communityStr);
        if (null == community && "DEFAULT".equals(communityStr)) {
            return StackCommunity.DM;
        } else {
            return community;
        }
    }

}
