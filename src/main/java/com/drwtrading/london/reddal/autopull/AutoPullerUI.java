package com.drwtrading.london.reddal.autopull;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class AutoPullerUI implements IAutoPullCallbacks {

    private final AutoPuller autoPuller;
    private final WebSocketViews<IAutoPullerView> views = new WebSocketViews<>(IAutoPullerView.class, this);
    private final Set<IAutoPullerView> viewSet = new HashSet<>();

    public AutoPullerUI(final AutoPuller autoPuller) {
        this.autoPuller = autoPuller;
        autoPuller.setCallbacks(this);
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {
        final IAutoPullerView view = views.register(connected);
        viewSet.add(view);
        onConnected(view);
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
            autoPuller.disableAllRules();
        }
    }

    @FromWebSocketView
    public void writeRule(final String ruleID, final String symbol, final String side, final String fromPrice, final String toPrice,
            final String priceCondition, final String conditionSide, final String qtyCondition, final String qtyThreshold) {

        final Long id = "NEW".equals(ruleID) ? PullRule.nextID() : Long.valueOf(ruleID);

        final PullRule pullRule = new PullRule(id, symbol,
                new OrderSelectionPriceRangeSelection(symbol, BookSide.valueOf(side), parsePx(fromPrice), parsePx(toPrice)),
                new MktConditionQtyAtPriceCondition(symbol, BookSide.valueOf(conditionSide), parsePx(priceCondition),
                        MktConditionConditional.valueOf(qtyCondition), Integer.valueOf(qtyThreshold)));
        autoPuller.addOrUpdateRule(pullRule);
        displayRule(views.all(), autoPuller.getRule(pullRule.ruleID));
    }

    @FromWebSocketView
    public void deleteRule(final String ruleID) {
        autoPuller.deleteRule(Long.valueOf(ruleID));
        views.all().removeRule(ruleID);
    }

    @FromWebSocketView
    public void startRule(final String ruleID, final WebSocketInboundData data) {
        final String username = data.getClient().getUserName();
        final Long id = Long.valueOf(ruleID);
        enableRuleIfNoInstantPull(username, id);
    }

    @FromWebSocketView
    public void startAllRules(final WebSocketInboundData data) {
        final List<Long> ids = autoPuller.getRules().values().stream().map(e -> e.pullRule.ruleID).collect(Collectors.toList());
        final String username = data.getClient().getUserName();
        for (final Long id : ids) {
            enableRuleIfNoInstantPull(username, id);
        }
    }

    @FromWebSocketView
    public void stopAllRules() {
        autoPuller.disableAllRules();
        displayAllRules(views.all());
    }

    @FromWebSocketView
    public void stopRule(final String ruleID) {
        final Long id = Long.valueOf(ruleID);
        final AutoPullerEnabledPullRule pullRule = autoPuller.disableRule(id);
        displayRule(views.all(), pullRule);
    }

    private void onConnected(final IAutoPullerView view) {
        updateGlobals(view);
        displayAllRules(view);
    }

    private void enableRuleIfNoInstantPull(final String username, final Long id) {
        final AutoPullerEnabledPullRule rule = autoPuller.getRule(id);
        if (0 == autoPuller.getPullCount(rule)) {
            autoPuller.enableRule(username, id);
        }
        displayRule(views.all(), rule);
    }

    private void displayAllRules(final IAutoPullerView view) {
        autoPuller.getRules().forEach((id, enabledPullRule) -> displayRule(view, enabledPullRule));
    }

    private void updateGlobals(final IAutoPullerView view) {

        final List<String> symbols = autoPuller.getRelevantSymbols();
        symbols.sort(Comparator.naturalOrder());
        final Map<String, List<String>> relevantPrices = new HashMap<>();
        for (final String symbol : symbols) {
            final List<String> priceList = autoPuller.getMDPrices(symbol).stream().map(AutoPullerUI::formatPx).collect(Collectors.toList());
            relevantPrices.put(symbol, priceList);
        }
        view.updateGlobals(symbols, relevantPrices, relevantPrices);
    }

    private void displayRule(final IAutoPullerView view, final AutoPullerEnabledPullRule enabledPullRule) {
        final PullRule rule = enabledPullRule.getPullRule();
        final int pullCount = autoPuller.getPullCount(enabledPullRule);
        view.displayRule(Long.toString(rule.ruleID), rule.symbol, rule.orderSelection.side.toString(),
                formatPx(rule.orderSelection.fromPrice), formatPx(rule.orderSelection.toPrice), formatPx(rule.mktCondition.price),
                rule.mktCondition.side.toString(), rule.mktCondition.qtyCondition.toString(),
                Integer.toString(rule.mktCondition.qtyThreshold), enabledPullRule.isEnabled(),
                enabledPullRule.getEnabledByUser() != null ? enabledPullRule.getEnabledByUser() : "", pullCount);
    }

    static String formatPx(final long price) {
        return new BigDecimal(price).movePointLeft(9).stripTrailingZeros().toPlainString();
    }

    static long parsePx(final String price) {
        return new BigDecimal(price).movePointRight(9).longValue();
    }

    @Override
    public void runRefreshView(final String message) {
        if (!viewSet.isEmpty()) {
            onConnected(views.all());
        }
        if (null != message) {
            views.all().showMessage(message);
        }
    }

    @Override
    public void ruleFired(final AutoPullerEnabledPullRule rule) {
        displayRule(views.all(), rule);
        views.all().ruleFired(Long.toString(rule.getPullRule().ruleID));
    }
}
