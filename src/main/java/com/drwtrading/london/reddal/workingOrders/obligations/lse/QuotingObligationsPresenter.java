package com.drwtrading.london.reddal.workingOrders.obligations.lse;

//import com.drwtrading.jetlang.autosubscribe.Subscribe;
//import com.drwtrading.london.eeif.utils.io.SelectIO;
//import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
//import com.drwtrading.london.eeif.utils.time.IClock;
//import com.drwtrading.london.fastui.FastUiOutbound;
//import com.drwtrading.london.photons.selecta.State;
//import com.drwtrading.london.photons.selecta.StrategyState;
//import com.drwtrading.london.photons.selecta.StrategyType;
//import com.drwtrading.london.selecta.UIConnectedUser;
//import com.drwtrading.london.selecta.UIView;
//import com.drwtrading.london.selecta.UiManager;
//import com.drwtrading.websockets.WebSocketConnected;
//import com.drwtrading.websockets.WebSocketDisconnected;
//import com.drwtrading.websockets.WebSocketInboundData;
//import org.jetlang.fibers.Fiber;
//
//import java.util.Calendar;
//import java.util.HashMap;
//import java.util.Map;
//import java.util.concurrent.TimeUnit;

public class QuotingObligationsPresenter {
//    implements UiManager.UiCallbacks {
//
//    private static final int CHECK_OBLIGATION_PERIOD_MILLIS = 30000;
//
//    private static final String UI_TITLE = "Quote Obligations.";
//    private static final int MIN_PERCENT = 90;
//
//    private final IClock clock;
//    private final long minMilliSinceMidnight;
//    private final long maxMilliSinceMidnight;
//    private final long sysStartMillisSinceMidnight;
//
//    private final UiManager uiManager;
//
//    private final Map<String, QuotingObligationState> obligations;
//
//    public QuotingObligationsPresenter(final SelectIO clock, final Fiber ui) {
//
//        this.clock = clock;
//
//        final Calendar cal = DateTimeUtil.getCalendar();
//        cal.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);
//
//        DateTimeUtil.setToTimeOfDay(cal, 8, 0, 0, 0);
//        this.minMilliSinceMidnight = cal.getTimeInMillis() - clock.getMillisAtMidnightUTC();
//
//        DateTimeUtil.setToTimeOfDay(cal, 16, 35, 0, 0);
//        this.maxMilliSinceMidnight = cal.getTimeInMillis() - clock.getMillisAtMidnightUTC();
//
//        this.sysStartMillisSinceMidnight = getNowMilliSinceMidnightNow();
//
//        this.uiManager = new UiManager(this);
//
//        this.obligations = new HashMap<>();
//
//        ui.scheduleWithFixedDelay(this::checkObligations, CHECK_OBLIGATION_PERIOD_MILLIS, CHECK_OBLIGATION_PERIOD_MILLIS,
//                TimeUnit.MILLISECONDS);
//    }
//
//    private long getNowMilliSinceMidnightNow() {
//        final long result = clock.getMillisSinceMidnightUTC();
//        final long morningLimited = Math.max(result, minMilliSinceMidnight);
//        return Math.min(morningLimited, maxMilliSinceMidnight);
//    }
//
//    private void checkObligations() {
//        uiManager.ui.eval("checkWarning()");
//    }
//
//    public void setStrategyState(final StrategyState state) {
//
//        if (StrategyType.QUOTING == state.getStrategy()) {
//            final QuotingObligationState obligation = getObligation(state.getSymbol(), State.ON == state.getState());
//            setObligation(uiManager.ui, uiManager.view, obligation);
//        }
//    }
//
//    @Override
//    public void onUserConnected(final UIConnectedUser user) {
//
//        user.view.setName(UI_TITLE);
//        for (final QuotingObligationState state : obligations.values()) {
//            setObligation(user.ui, user.view, state);
//        }
//    }
//
//    @Override
//    public void onUserDisconnected(final UIConnectedUser user) {
//        // no-op
//    }
//
//    @Override
//    public void onUserHeartbeat(final UIConnectedUser user) {
//        // no-op
//    }
//
//    @Override
//    public void onUserFiltersUpdated(final UIConnectedUser user) {
//        // no-op
//    }
//
//    @Subscribe
//    public void on(final WebSocketConnected webSocketConnected) {
//        uiManager.on(webSocketConnected);
//    }
//
//    @Subscribe
//    public void on(final WebSocketDisconnected disconnected) {
//        uiManager.on(disconnected);
//    }
//
//    @Subscribe
//    public void on(final WebSocketInboundData inboundData) {
//        uiManager.on(inboundData);
//    }
//
//    private QuotingObligationState getObligation(final String symbol, final boolean isOn) {
//
//        final long nowMilliSinceMidnight = getNowMilliSinceMidnightNow();
//        final QuotingObligationState result = obligations.get(symbol);
//        if (null == result) {
//            final QuotingObligationState newState =
//                    new QuotingObligationState(symbol, sysStartMillisSinceMidnight, nowMilliSinceMidnight, isOn);
//            obligations.put(symbol, newState);
//            return newState;
//        } else {
//            result.setState(nowMilliSinceMidnight, isOn);
//            return result;
//        }
//    }
//
//    private void setObligation(final FastUiOutbound ui, final UIView view, final QuotingObligationState obligation) {
//
//        final String symbol = obligation.getSymbol();
//        final String key = obligation.getKey();
//        view.addRow(key);
//        ui.txt(key + "_symbol", symbol);
//        uiManager.clickable(key + "_symbol", (id, data, user1) -> user1.view.launchLadder(symbol));
//        setRowDetails(ui, obligation);
//
//        uiManager.ui.flush();
//    }
//
//    public void flushRunnable() {
//
//        final long nowMilliSinceMidnight = getNowMilliSinceMidnightNow();
//        for (final QuotingObligationState obligation : obligations.values()) {
//
//            obligation.updatePercent(nowMilliSinceMidnight);
//            setRowDetails(uiManager.ui, obligation);
//        }
//
//        uiManager.ui.flush();
//    }
//
//    private static void setRowDetails(final FastUiOutbound ui, final QuotingObligationState obligation) {
//
//        final String key = obligation.getKey();
//        ui.txt(key + "_percentageOn", Long.toString(obligation.getOnPercent()));
//        ui.cls(key, "strategyOn", obligation.isOn());
//        ui.cls(key, "obligationFail", obligation.getOnPercent() < MIN_PERCENT);
//    }
}
