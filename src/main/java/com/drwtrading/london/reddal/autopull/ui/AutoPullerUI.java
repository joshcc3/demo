package com.drwtrading.london.reddal.autopull.ui;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.autopull.AutoPullerRuleState;
import com.drwtrading.london.reddal.autopull.msgs.cmds.AutoPullerDeleteRule;
import com.drwtrading.london.reddal.autopull.msgs.cmds.AutoPullerPriceRefreshRequest;
import com.drwtrading.london.reddal.autopull.msgs.cmds.AutoPullerSafeStartAll;
import com.drwtrading.london.reddal.autopull.msgs.cmds.AutoPullerSafeStartRule;
import com.drwtrading.london.reddal.autopull.msgs.cmds.AutoPullerSetRule;
import com.drwtrading.london.reddal.autopull.msgs.cmds.AutoPullerStopAll;
import com.drwtrading.london.reddal.autopull.msgs.cmds.AutoPullerStopRule;
import com.drwtrading.london.reddal.autopull.msgs.cmds.IAutoPullerCmd;
import com.drwtrading.london.reddal.autopull.msgs.updates.IAutoPullerUpdateHandler;
import com.drwtrading.london.reddal.autopull.rules.MktConditionConditional;
import com.drwtrading.london.reddal.autopull.rules.MktConditionQtyAtPriceCondition;
import com.drwtrading.london.reddal.autopull.rules.OrderSelectionPriceRangeSelection;
import com.drwtrading.london.reddal.autopull.rules.PullRule;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.channels.Publisher;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class AutoPullerUI implements IAutoPullerUpdateHandler {

    private static final long STOP_ALL_PERIOD_MILLIS = 60000;
    private static final AutoPullerPriceRefreshRequest REFRESH_REQUEST = new AutoPullerPriceRefreshRequest();

    private final AutoPullPersistence persistence;
    private final Publisher<IAutoPullerCmd> cmdPublisher;

    private final WebSocketViews<IAutoPullerView> views = new WebSocketViews<>(IAutoPullerView.class, this);
    private final Set<IAutoPullerView> viewSet = new HashSet<>();

    private final Map<String, List<String>> relevantPrices;
    private final LongMap<AutoPullerRuleState> ruleStates;

    private final DecimalFormat priceDF;

    public AutoPullerUI(final AutoPullPersistence persistence, final Publisher<IAutoPullerCmd> cmdPublisher) {

        this.persistence = persistence;
        this.cmdPublisher = cmdPublisher;

        this.relevantPrices = new TreeMap<>();
        this.ruleStates = new LongMap<>();

        this.priceDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 0, 5);
    }

    public void start(final SelectIO selectIO) {

        for (final PullRule rule : persistence.getPullRules().values()) {
            sendRule(rule);
            relevantPrices.put(rule.symbol, Collections.emptyList());
        }

        final Calendar cal = DateTimeUtil.getCalendar();
        cal.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);
        DateTimeUtil.setToTimeOfDay(cal, 20, 57, 0, 0);
        selectIO.addClockActionMilliSinceUTC(cal.getTimeInMillis(), this::autoNightStopAll);
    }

    private long autoNightStopAll() {

        stopAllRules();
        views.all().showMessage("Disabled for night-time");
        return STOP_ALL_PERIOD_MILLIS;
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {

        final IAutoPullerView view = views.register(connected);
        viewSet.add(view);

        views.all().updateGlobals(relevantPrices.keySet(), relevantPrices, relevantPrices);

        for (final LongMapNode<AutoPullerRuleState> ruleStateNode : ruleStates) {
            final AutoPullerRuleState ruleState = ruleStateNode.getValue();
            displayRule(view, ruleState);
        }

        cmdPublisher.publish(REFRESH_REQUEST);
    }

    @Subscribe
    public void on(final WebSocketInboundData inbound) {

        views.invoke(inbound);
    }

    @Subscribe
    public void on(final WebSocketDisconnected disconnected) {

        final IAutoPullerView view = views.unregister(disconnected);
        viewSet.remove(view);
        if (viewSet.isEmpty()) {
            stopAllRules();
        }
    }

    @FromWebSocketView
    public void writeRule(final String ruleID, final String symbol, final String side, final String fromPrice, final String toPrice,
            final String priceCondition, final String conditionSide, final String qtyCondition, final String qtyThreshold) {

        final long id = "NEW".equals(ruleID) ? PullRule.nextID() : Long.parseLong(ruleID);

        final PullRule pullRule = new PullRule(id, symbol,
                new OrderSelectionPriceRangeSelection(symbol, BookSide.valueOf(side), parsePx(fromPrice), parsePx(toPrice)),
                new MktConditionQtyAtPriceCondition(symbol, BookSide.valueOf(conditionSide), parsePx(priceCondition),
                        MktConditionConditional.valueOf(qtyCondition), Integer.parseInt(qtyThreshold)));
        persistence.updateRule(pullRule);

        sendRule(pullRule);
    }

    private void sendRule(final PullRule pullRule) {

        final AutoPullerSetRule addRuleCmd = new AutoPullerSetRule(pullRule);
        cmdPublisher.publish(addRuleCmd);
    }

    @FromWebSocketView
    public void deleteRule(final String ruleIdentifier) {

        final long ruleID = Long.parseLong(ruleIdentifier);
        persistence.deleteRule(ruleID);

        final AutoPullerDeleteRule deleteRule = new AutoPullerDeleteRule(ruleID);
        cmdPublisher.publish(deleteRule);
    }

    @FromWebSocketView
    public void startRule(final String id, final WebSocketInboundData data) {

        final long ruleID = Long.parseLong(id);
        final String username = data.getClient().getUserName();

        final AutoPullerSafeStartRule startRule = new AutoPullerSafeStartRule(ruleID, username);
        cmdPublisher.publish(startRule);
    }

    @FromWebSocketView
    public void startAllRules(final WebSocketInboundData data) {

        final String username = data.getClient().getUserName();

        final AutoPullerSafeStartAll startAll = new AutoPullerSafeStartAll(username);
        cmdPublisher.publish(startAll);
    }

    @FromWebSocketView
    public void stopRule(final String id) {

        final long ruleID = Long.parseLong(id);
        final AutoPullerStopRule stopRule = new AutoPullerStopRule(ruleID);
        cmdPublisher.publish(stopRule);
    }

    @FromWebSocketView
    public void stopAllRules() {

        final AutoPullerStopAll stopAll = new AutoPullerStopAll();
        cmdPublisher.publish(stopAll);
    }

    @Override
    public void setRuleState(final AutoPullerRuleState ruleState) {

        ruleStates.put(ruleState.rule.ruleID, ruleState);
        displayRule(views.all(), ruleState);
    }

    @Override
    public void ruleDeleted(final long ruleID) {

        ruleStates.remove(ruleID);
        views.all().removeRule(Long.toString(ruleID));
    }

    @Override
    public void ruleFired(final AutoPullerRuleState ruleState) {

        setRuleState(ruleState);

        views.all().ruleFired(Long.toString(ruleState.rule.ruleID));
    }

    @Override
    public void refreshPrices(final Map<String, List<Long>> symbolPrices) {

        for (final Map.Entry<String, List<Long>> symbolPrice : symbolPrices.entrySet()) {

            final String symbol = symbolPrice.getKey();
            final List<String> priceList = symbolPrice.getValue().stream().map(this::formatPx).collect(Collectors.toList());
            relevantPrices.put(symbol, priceList);
        }
        views.all().updateGlobals(relevantPrices.keySet(), relevantPrices, relevantPrices);
    }

    private void displayRule(final IAutoPullerView view, final AutoPullerRuleState enabledPullRule) {

        final PullRule rule = enabledPullRule.rule;
        final int pullCount = enabledPullRule.matchedOrders;
        final String associatedUser = null == enabledPullRule.associatedUser ? "" : enabledPullRule.associatedUser;

        view.displayRule(Long.toString(rule.ruleID), rule.symbol, rule.orderSelection.side.toString(),
                formatPx(rule.orderSelection.fromPrice), formatPx(rule.orderSelection.toPrice), formatPx(rule.mktCondition.price),
                rule.mktCondition.side.toString(), rule.mktCondition.qtyCondition.toString(),
                Integer.toString(rule.mktCondition.qtyThreshold), enabledPullRule.isEnabled, associatedUser, pullCount);
    }

    private String formatPx(final long price) {
        return priceDF.format(price / (double) Constants.NORMALISING_FACTOR);
    }

    private static long parsePx(final String price) {
        return new BigDecimal(price).movePointRight(9).longValue();
    }
}
