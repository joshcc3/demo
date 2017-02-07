package com.drwtrading.london.reddal.autopull;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.core.Scheduler;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class AutoPullerUI {

    final AutoPuller autoPuller;
    private final Scheduler scheduler;
    final WebSocketViews<View> views = new WebSocketViews<>(View.class, this);

    public AutoPullerUI(AutoPuller autoPuller, Scheduler scheduler) {
        this.autoPuller = autoPuller;
        this.scheduler = scheduler;
        autoPuller.setRefreshCallback(this::refreshRules);
    }

    @Subscribe
    public void on(WebSocketConnected connected) {
        View view = views.register(connected);
        onConnected(view);
    }

    @Subscribe
    public void on(WebSocketInboundData inbound) {
        views.invoke(inbound);
    }

    @Subscribe
    public void on(WebSocketDisconnected disconnected) {
        View view = views.unregister(disconnected);
    }


    @FromWebSocketView
    public void writeRule(
            String ruleID, String symbol, String side, String fromPrice, String toPrice,
            String priceCondition, String conditionSide, String qtyCondition,
            String qtyThreshold) {

        Long id = "NEW".equals(ruleID) ? PullRule.nextID() : Long.valueOf(ruleID);

        PullRule pullRule = new PullRule(
                id,
                symbol,
                new OrderSelection.PriceRangeSelection(
                        symbol, BookSide.valueOf(side),
                        parsePx(fromPrice), parsePx(toPrice)
                ),
                new MktCondition.QtyAtPriceCondition(
                        symbol, BookSide.valueOf(conditionSide),
                        parsePx(priceCondition), MktCondition.Condition.valueOf(qtyCondition),
                        Integer.valueOf(qtyThreshold)
                )
        );
        autoPuller.addOrUpdateRule(pullRule);
        displayRule(views.all(), autoPuller.getRule(pullRule.ruleID));
    }

    @FromWebSocketView
    public void deleteRule(String ruleID) {
        autoPuller.deleteRule(Long.valueOf(ruleID));
        views.all().removeRule(ruleID);
    }

    @FromWebSocketView
    public void startRule(String ruleID, WebSocketInboundData data) {
        String username = data.getClient().getUserName();
        Long id = Long.valueOf(ruleID);
        AutoPuller.EnabledPullRule rule = autoPuller.enableRule(username, id);
        displayRule(views.all(), rule);
    }

    @FromWebSocketView
    public void stopRule(String ruleID) {
        Long id = Long.valueOf(ruleID);
        AutoPuller.EnabledPullRule pullRule = autoPuller.disableRule(id);
        displayRule(views.all(), pullRule);
    }

    public void refreshRules() {
        onConnected(views.all());
    }

    private void onConnected(View view) {
        updateGlobals(view);
        displayAllRules(view);
    }

    private void displayAllRules(View view) {
        autoPuller.getRules().forEach((id, enabledPullRule) -> displayRule(view, enabledPullRule));
    }

    private void updateGlobals(View view) {
        List<String> symbols = autoPuller.getRelevantSymbols();
        Map<String, List<String>> orderPrices = new HashMap<>();
        Map<String, List<String>> relevantPrices = new HashMap<>();
        for (String symbol : symbols) {
            List<String> list = autoPuller.getWorkingOrderPrices(symbol).stream()
                    .map(AutoPullerUI::formatPx)
                    .collect(Collectors.toList());
            orderPrices.put(symbol, list);
            List<String> priceList = autoPuller.getMDPrices(symbol).stream().map(AutoPullerUI::formatPx).collect(Collectors.toList());
            if (priceList.isEmpty()) {
                retryLater();
            }
            relevantPrices.put(symbol, priceList);
        }
        view.updateGlobals(symbols, relevantPrices, relevantPrices);
    }

    private void retryLater() {
        scheduler.schedule(this::refreshRules, 2, TimeUnit.SECONDS);
    }

    private void displayRule(View view, AutoPuller.EnabledPullRule enabledPullRule) {
        PullRule rule = enabledPullRule.getPullRule();
        int pullCount = autoPuller.getPullCount(enabledPullRule);
        view.displayRule(Long.toString(rule.ruleID), rule.symbol, rule.orderSelection.side.toString(),
                formatPx(rule.orderSelection.fromPrice),
                formatPx(rule.orderSelection.toPrice),
                formatPx(rule.mktCondition.price),
                rule.mktCondition.side.toString(),
                rule.mktCondition.qtyCondition.toString(),
                Integer.toString(rule.mktCondition.qtyThreshold),
                enabledPullRule.isEnabled(),
                enabledPullRule.getEnabledByUser() != null ? enabledPullRule.getEnabledByUser() : "",
                pullCount
        );
    }

    static String formatPx(long price) {
        return new BigDecimal(price).movePointLeft(9).stripTrailingZeros().toPlainString();
    }

    static long parsePx(String price) {
        return new BigDecimal(price).movePointRight(9).longValue();
    }

    public interface View {
        void updateGlobals(List<String> relevantSymbols, Map<String, List<String>> symbolToWorkingPrice, Map<String, List<String>> symbolToPossiblePrices);

        void displayRule(String key, String symbol,
                         String side,
                         String orderPriceFrom,
                         String orderPriceTo,
                         String conditionPrice,
                         String conditionSide,
                         String qtyCondition,
                         String qtyThreshold,
                         boolean enabled,
                         String enabledByUser,
                         int pullCount);

        void removeRule(String key);
    }

}
