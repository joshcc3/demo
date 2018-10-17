package com.drwtrading.london.reddal.autopull.onMD;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.reddal.autopull.AutoPullerRuleState;
import com.drwtrading.london.reddal.autopull.msgs.cmds.IAutoPullerCmdHandler;
import com.drwtrading.london.reddal.autopull.msgs.updates.AutoPullerPriceRefresh;
import com.drwtrading.london.reddal.autopull.msgs.updates.AutoPullerRuleDeleted;
import com.drwtrading.london.reddal.autopull.msgs.updates.AutoPullerRuleFired;
import com.drwtrading.london.reddal.autopull.msgs.updates.AutoPullerRuleStateUpdate;
import com.drwtrading.london.reddal.autopull.msgs.updates.IAutoPullerUpdate;
import com.drwtrading.london.reddal.autopull.rules.PullRule;
import com.drwtrading.london.reddal.data.ibook.IMDCallback;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
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

    private final MDSource mdSource;
    private final IMDSubscriber mdSubscriber;

    private final Publisher<RemoteOrderCommandToServer> commandPublisher;
    private final Publisher<IAutoPullerUpdate> updatePublisher;

    private final Map<String, HashSet<SourcedWorkingOrder>> workingOrders;
    private final Map<String, MDForSymbol> mdForSymbols;
    private final LongMap<AutoPullerPullRule> rulesByID;
    private final Map<String, LongMap<AutoPullerPullRule>> rulesBySymbol;
    private final LongMap<AutoPullerRuleState> lastRuleStates;

    public AutoPuller(final MDSource mdSource, final IMDSubscriber mdSubscriber,
            final Publisher<RemoteOrderCommandToServer> commandPublisher, final Publisher<IAutoPullerUpdate> updatePublisher) {

        this.mdSource = mdSource;
        this.mdSubscriber = mdSubscriber;

        this.commandPublisher = commandPublisher;
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
                prices.add(rule.pullRule.mktCondition.price);
                prices.add(rule.pullRule.orderSelection.fromPrice);
                prices.add(rule.pullRule.orderSelection.toPrice);
            }
        }

        final MDForSymbol md = mdForSymbols.get(symbol);
        if (null != md && null != md.getBook()) {

            final IBook<?> book = md.getBook();
            Long bidPrice = null;
            for (IBookLevel lvl = book.getBestBid(); lvl != null; lvl = lvl.next()) {
                bidPrice = lvl.getPrice();
            }

            Long askPrice = null;
            for (IBookLevel lvl = book.getBestAsk(); lvl != null; lvl = lvl.next()) {
                askPrice = lvl.getPrice();
            }

            if (bidPrice == null && null != askPrice) {
                bidPrice = askPrice;
            }
            if (askPrice == null && null != bidPrice) {
                askPrice = bidPrice;
            }
            if (bidPrice != null) {
                prices.add(book.getTickTable().subtractTicks(bidPrice, 5));
                prices.add(book.getTickTable().addTicks(askPrice, 5));
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

        final MDForSymbol mdForSymbol = addMDSubscription(rule.symbol);
        deleteRule(rule.ruleID);

        final AutoPullerPullRule newRule = new AutoPullerPullRule(rule, mdForSymbol);
        rulesByID.put(rule.ruleID, newRule);

        final LongMap<AutoPullerPullRule> symbolRules = MapUtils.getMappedLongMap(rulesBySymbol, rule.symbol);
        symbolRules.put(rule.ruleID, newRule);

        final Set<SourcedWorkingOrder> sourcedOrders = workingOrders.get(rule.symbol);
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
    public void safeStartAll(final String username) {

        for (final LongMapNode<AutoPullerPullRule> pullRule : rulesByID) {
            safeStart(pullRule.getValue(), username);
        }
    }

    @Override
    public void safeStartRule(final long ruleID, final String username) {

        final AutoPullerPullRule pullRule = rulesByID.get(ruleID);
        if (null != pullRule) {
            safeStart(pullRule, username);
        }
    }

    private void safeStart(final AutoPullerPullRule rule, final String username) {

        final Set<SourcedWorkingOrder> orders = workingOrders.get(rule.pullRule.symbol);

        final Collection<RemoteOrderCommandToServer> cmds = rule.getOrdersToPull(orders);

        if (cmds.isEmpty()) {
            rule.enable(username);
            runRule(orders, rule);
        } else {
            sendRuleStateUpdate(rule, cmds.size());
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

            final String symbol = rule.pullRule.symbol;

            final LongMap<AutoPullerPullRule> symbolRules = rulesBySymbol.get(symbol);
            symbolRules.remove(ruleID);

            if (symbolRules.isEmpty()) {
                rulesBySymbol.remove(symbol);
            }

            if (null != rule.md.getBook()) {
                final AutoPullerRuleDeleted ruleDeleted = new AutoPullerRuleDeleted(ruleID);
                updatePublisher.publish(ruleDeleted);
            }

            lastRuleStates.remove(rule.pullRule.ruleID);
        }
    }

    private void runSymbol(final String symbol) {

        final Set<SourcedWorkingOrder> workingOrders = this.workingOrders.get(symbol);
        final LongMap<AutoPullerPullRule> rules = rulesBySymbol.get(symbol);

        if (null != workingOrders && !workingOrders.isEmpty() && null != rules && !rules.isEmpty()) {

            for (final LongMapNode<AutoPullerPullRule> pullRuleNode : rules) {

                final AutoPullerPullRule pullRule = pullRuleNode.getValue();
                runRule(workingOrders, pullRule);
            }
        }
    }

    private void runRule(final Set<SourcedWorkingOrder> workingOrders, final AutoPullerPullRule rule) {

        final List<RemoteOrderCommandToServer> cancels = rule.getOrdersToPull(workingOrders);

        if (!cancels.isEmpty() && rule.isEnabled()) {

            for (final RemoteOrderCommandToServer cmd : cancels) {
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
            System.out.println("Book:");
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
