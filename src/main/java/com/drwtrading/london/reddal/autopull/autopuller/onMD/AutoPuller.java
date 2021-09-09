package com.drwtrading.london.reddal.autopull.autopuller.onMD;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.reddal.autopull.autopuller.AutoPullerRuleState;
import com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds.IAutoPullerCmdHandler;
import com.drwtrading.london.reddal.autopull.autopuller.msgs.updates.AutoPullerPriceRefresh;
import com.drwtrading.london.reddal.autopull.autopuller.msgs.updates.AutoPullerRuleDeleted;
import com.drwtrading.london.reddal.autopull.autopuller.msgs.updates.AutoPullerRuleFired;
import com.drwtrading.london.reddal.autopull.autopuller.msgs.updates.AutoPullerRuleStateUpdate;
import com.drwtrading.london.reddal.autopull.autopuller.msgs.updates.IAutoPullerUpdate;
import com.drwtrading.london.reddal.autopull.autopuller.rules.PullRule;
import com.drwtrading.london.reddal.data.ibook.IMDCallback;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import org.jetlang.channels.Publisher;
import org.joda.time.DateTime;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

public class AutoPuller implements IAutoPullerCmdHandler, IMDCallback {

    private static final int MAX_DEPTH_TO_DISPLAY = 25;

    private final MDSource mdSource;
    private final IMDSubscriber mdSubscriber;

    private final Publisher<IOrderCmd> commandPublisher;
    private final Publisher<LadderClickTradingIssue> cancelRejectMsgs;
    private final Publisher<IAutoPullerUpdate> updatePublisher;

    private final Map<String, HashSet<SourcedWorkingOrder>> workingOrders;
    private final Map<String, MDForSymbol> mdForSymbols;
    private final LongMap<AutoPullerPullRule> rulesByID;
    private final Map<String, LongMap<AutoPullerPullRule>> rulesBySymbol;
    private final LongMap<AutoPullerRuleState> lastRuleStates;

    public AutoPuller(final MDSource mdSource, final IMDSubscriber mdSubscriber, final Publisher<IOrderCmd> commandPublisher,
            final Publisher<LadderClickTradingIssue> cancelRejectMsgs, final Publisher<IAutoPullerUpdate> updatePublisher) {

        this.mdSource = mdSource;
        this.mdSubscriber = mdSubscriber;

        this.commandPublisher = commandPublisher;
        this.cancelRejectMsgs = cancelRejectMsgs;
        this.updatePublisher = updatePublisher;

        this.workingOrders = new HashMap<>();
        this.mdForSymbols = new HashMap<>();
        this.rulesByID = new LongMap<>();
        this.rulesBySymbol = new HashMap<>();
        this.lastRuleStates = new LongMap<>();
    }

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final WorkingOrder order = sourcedOrder.order;
        final String symbol = order.getSymbol();

        if (OrderType.GTC == order.getOrderType() && symbol.contains("-")) {

            final HashSet<SourcedWorkingOrder> sourcedOrders = MapUtils.getMappedSet(workingOrders, symbol);

            addMDSubscription(symbol);

            sourcedOrders.add(sourcedOrder);
            runSymbol(order.getSymbol());
        }
    }

    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final WorkingOrder order = sourcedOrder.order;
        final String symbol = order.getSymbol();
        final HashSet<SourcedWorkingOrder> sourcedOrders = workingOrders.get(symbol);

        if (null != sourcedOrders) {
            sourcedOrders.remove(sourcedOrder);
            runSymbol(order.getSymbol());
        }
    }

    @Override
    public void bookUpdated(final MDForSymbol mdForSymbol) {
        runSymbol(mdForSymbol.symbol);
    }

    @Override
    public void priceRefreshRequest() {

        final Map<String, List<Long>> symbolPrices = new HashMap<>();

        for (final String symbol : mdForSymbols.keySet()) {

            final List<Long> prices = getMDPrices(symbol);
            symbolPrices.put(symbol, prices);
        }

        final AutoPullerPriceRefresh priceRefresh = new AutoPullerPriceRefresh(symbolPrices);
        updatePublisher.publish(priceRefresh);
    }

    private List<Long> getMDPrices(final String symbol) {

        final TreeSet<Long> prices = new TreeSet<>(Comparator.reverseOrder());

        final Set<SourcedWorkingOrder> sourcedOrders = workingOrders.get(symbol);
        if (null != sourcedOrders) {

            addAllPrices(sourcedOrders, prices);
        }

        final LongMap<AutoPullerPullRule> pullRules = rulesBySymbol.get(symbol);
        if (null != pullRules) {

            for (final LongMapNode<AutoPullerPullRule> pullRuleNode : pullRules) {

                final AutoPullerPullRule rule = pullRuleNode.getValue();

                final PullRule pullRule = rule.pullRule;
                if (pullRule.mdSymbol.equals(symbol)) {
                    prices.add(rule.pullRule.mktCondition.price);
                }
                if (pullRule.orderSymbol.equals(symbol)) {
                    prices.add(rule.pullRule.orderSelection.fromPrice);
                    prices.add(rule.pullRule.orderSelection.toPrice);
                }
            }
        }

        final MDForSymbol md = mdForSymbols.get(symbol);
        if (null != md && null != md.getBook()) {

            final IBook<?> book = md.getBook();
            IBookLevel bestBid = book.getBestBid();
            IBookLevel bestAsk = book.getBestAsk();

            if (null == bestBid && null != bestAsk) {
                bestBid = bestAsk;
            }
            if (null != bestBid && null == bestAsk) {
                bestAsk = bestBid;
            }
            if (bestBid != null) {
                prices.add(book.getTickTable().subtractTicks(bestBid.getPrice(), MAX_DEPTH_TO_DISPLAY));
                prices.add(book.getTickTable().addTicks(bestAsk.getPrice(), MAX_DEPTH_TO_DISPLAY));
            }

            if (!prices.isEmpty()) {
                for (long price = prices.first(); prices.last() <= price; price = book.getTickTable().subtractTicks(price, 1)) {
                    prices.add(price);
                }
            }
        }

        return new ArrayList<>(prices);
    }

    private static void addAllPrices(final Set<SourcedWorkingOrder> orders, final Set<Long> prices) {

        for (final SourcedWorkingOrder order : orders) {
            prices.add(order.order.getPrice());
        }
    }

    @Override
    public void setRule(final PullRule rule) {

        final MDForSymbol mdForSymbol = addMDSubscription(rule.mdSymbol);
        deleteRule(rule.ruleID);

        final AutoPullerPullRule newRule = new AutoPullerPullRule(rule, mdForSymbol);
        rulesByID.put(rule.ruleID, newRule);

        final LongMap<AutoPullerPullRule> mdSymbolRules = MapUtils.getMappedLongMap(rulesBySymbol, rule.mdSymbol);
        mdSymbolRules.put(rule.ruleID, newRule);

        if (!rule.mdSymbol.equals(rule.orderSymbol)) {
            final LongMap<AutoPullerPullRule> orderSymbolRules = MapUtils.getMappedLongMap(rulesBySymbol, rule.orderSymbol);
            orderSymbolRules.put(rule.ruleID, newRule);
        }

        final Set<SourcedWorkingOrder> sourcedOrders = workingOrders.get(rule.orderSymbol);
        runRule(sourcedOrders, newRule);
    }

    private MDForSymbol addMDSubscription(final String symbol) {

        final MDForSymbol oldMDForSystem = mdForSymbols.get(symbol);

        if (null == oldMDForSystem) {
            final MDForSymbol mdForSymbol = mdSubscriber.subscribeForMDCallbacks(symbol, this);
            mdForSymbols.put(symbol, mdForSymbol);
            return mdForSymbol;
        } else {
            return oldMDForSystem;
        }
    }

    @Override
    public void safeStartAll(final User user) {

        for (final LongMapNode<AutoPullerPullRule> pullRule : rulesByID) {
            safeStart(pullRule.getValue(), user);
        }
    }

    @Override
    public void safeStartRule(final long ruleID, final User user) {

        final AutoPullerPullRule pullRule = rulesByID.get(ruleID);
        if (null != pullRule) {
            safeStart(pullRule, user);
        }
    }

    private void safeStart(final AutoPullerPullRule rule, final User user) {

        if (null != rule.md) {

            final Set<SourcedWorkingOrder> orders = workingOrders.get(rule.pullRule.orderSymbol);

            final Collection<IOrderCmd> cmds = rule.getOrdersToPull(cancelRejectMsgs, orders);

            if (cmds.isEmpty()) {
                rule.enable(user);
                runRule(orders, rule);
            } else {
                sendRuleStateUpdate(rule, cmds.size());
            }
        }
    }

    @Override
    public void stopAll() {

        for (final LongMapNode<AutoPullerPullRule> pullRule : rulesByID) {
            stopRule(pullRule.getValue());
        }
    }

    @Override
    public void stopRule(final long ruleID) {

        final AutoPullerPullRule pullRule = rulesByID.get(ruleID);
        stopRule(pullRule);
    }

    private void stopRule(final AutoPullerPullRule rule) {

        rule.disable();
        if (null != rule.md.getBook()) {
            sendRuleStateUpdate(rule, 0);
        }
    }

    @Override
    public void deleteRule(final long ruleID) {

        final AutoPullerPullRule rule = rulesByID.remove(ruleID);
        if (null != rule) {

            deleteRule(rule.pullRule.mdSymbol, ruleID);

            if (!rule.pullRule.mdSymbol.equals(rule.pullRule.orderSymbol)) {
                deleteRule(rule.pullRule.orderSymbol, ruleID);
            }

            if (null != rule.md.getBook()) {
                final AutoPullerRuleDeleted ruleDeleted = new AutoPullerRuleDeleted(ruleID);
                updatePublisher.publish(ruleDeleted);
            }

            lastRuleStates.remove(rule.pullRule.ruleID);
        }
    }

    private void deleteRule(final String symbol, final long ruleID) {

        final LongMap<AutoPullerPullRule> symbolRules = rulesBySymbol.get(symbol);
        symbolRules.remove(ruleID);

        if (symbolRules.isEmpty()) {
            rulesBySymbol.remove(symbol);
        }
    }

    private void runSymbol(final String symbol) {

        final LongMap<AutoPullerPullRule> rules = rulesBySymbol.get(symbol);

        if (null != rules && !rules.isEmpty()) {

            for (final LongMapNode<AutoPullerPullRule> pullRuleNode : rules) {

                final AutoPullerPullRule pullRule = pullRuleNode.getValue();
                final Set<SourcedWorkingOrder> workingOrders = this.workingOrders.get(pullRule.pullRule.orderSymbol);

                runRule(workingOrders, pullRule);
            }
        }
    }

    private void runRule(final Set<SourcedWorkingOrder> workingOrders, final AutoPullerPullRule rule) {

        if (null != rule.md.getBook()) {

            final List<IOrderCmd> cancels = rule.getOrdersToPull(cancelRejectMsgs, workingOrders);

            if (!cancels.isEmpty() && rule.isEnabled()) {

                for (final IOrderCmd cmd : cancels) {
                    commandPublisher.publish(cmd);
                }

                rule.disable();

                final AutoPullerRuleState ruleState =
                        new AutoPullerRuleState(rule.pullRule, mdSource, rule.isEnabled(), rule.getAssociatedUser(), cancels.size());
                lastRuleStates.put(rule.pullRule.ruleID, ruleState);

                final AutoPullerRuleFired ruleFired = new AutoPullerRuleFired(ruleState);
                updatePublisher.publish(ruleFired);

                final IBook<?> book = rule.md.getBook();
                System.out.println("---- Auto puller Fired --- " + new DateTime());
                System.out.println("Rule:\n" + rule.pullRule);
                System.out.println("Book: " + book.getSymbol());
                System.out.println("\tinstid " + book.getInstID() + ", source " + book.getSourceExch() + " status " + book.getStatus());
                System.out.println("\tseqno " + book.getLastPacketSeqNum() + " ref time " + book.getReferenceNanoSinceMidnightUTC());
                System.out.println("\tvalid " + book.isValid());
                debugPrintLevels(book.getBestBid());
                debugPrintLevels(book.getBestAsk());
                System.out.println("Working orders: \n" + new ArrayList<>(workingOrders));
            } else {

                sendRuleStateUpdate(rule, cancels.size());
            }
        }
    }

    private void sendRuleStateUpdate(final AutoPullerPullRule rule, final int ordersToCancel) {

        final AutoPullerRuleState ruleState =
                new AutoPullerRuleState(rule.pullRule, mdSource, rule.isEnabled(), rule.getAssociatedUser(), ordersToCancel);
        final AutoPullerRuleState prevRuleState = lastRuleStates.put(rule.pullRule.ruleID, ruleState);

        if (!ruleState.equals(prevRuleState)) {
            final AutoPullerRuleStateUpdate ruleUpdate = new AutoPullerRuleStateUpdate(ruleState);
            updatePublisher.publish(ruleUpdate);
        }
    }

    private static void debugPrintLevels(final IBookLevel bid) {

        for (IBookLevel lvl = bid; lvl != null; lvl = lvl.next()) {
            System.out.print("\t" + lvl.getSide() + '\t' + lvl.getPrice() + '\t' + lvl.getQty() + '\t');
            if (lvl instanceof IBookLevelWithOrders) {
                final IBookLevelWithOrders lvo = (IBookLevelWithOrders) lvl;
                for (IBookOrder o = lvo.getFirstOrder(); o != null; o = o.next()) {
                    System.out.print("(" + o.getOrderID() + ' ' + o.getRemainingQty() + ')');
                }
            }
            System.out.print("\n");
        }
    }

}
