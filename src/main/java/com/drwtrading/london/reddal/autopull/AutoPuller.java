package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

public class AutoPuller {

    private final Instant DISABLE_TIME = new DateTime().withHourOfDay(20).withMinuteOfHour(57).withSecondOfMinute(0).toInstant();

    private final Publisher<RemoteOrderCommandToServer> commandPublisher;
    private final PullerBookSubscriber bookSubscriber;
    private final AutoPullPersistence persistence;

    private final Map<String, HashSet<SourcedWorkingOrder>> orders;
    private final Map<String, IBook<?>> md = new HashMap<>();
    private final Multimap<String, AutoPullerEnabledPullRule> rulesBySymbol = HashMultimap.create();
    private final Map<Long, AutoPullerEnabledPullRule> rulesByID = new TreeMap<>();
    private IAutoPullCallbacks refreshCallback = IAutoPullCallbacks.DEFAULT;

    public AutoPuller(final Publisher<RemoteOrderCommandToServer> commandPublisher, final PullerBookSubscriber bookSubscriber,
            final AutoPullPersistence persistence) {

        this.commandPublisher = commandPublisher;
        this.bookSubscriber = bookSubscriber;
        this.persistence = persistence;

        this.orders = new HashMap<>();

        bookSubscriber.setCreatedCallback(this::onNewBook);
        persistence.getPullRules().values().forEach(this::addOrUpdateRule);
    }

    public void updateWorkingOrder(final SourcedWorkingOrder sourcedOrder, final boolean isAlive) {

        final WorkingOrder order = sourcedOrder.order;
        final String symbol = order.getSymbol();

        if (OrderType.GTC == order.getOrderType() && isSpread(symbol)) {

            final boolean isNewSymbol = createIfNewSymbol(symbol);

            final HashSet<SourcedWorkingOrder> sourcedOrders = MapUtils.getMappedSet(orders, symbol);
            if (isAlive) {
                sourcedOrders.add(sourcedOrder);
            } else {
                sourcedOrders.remove(sourcedOrder);
                if (sourcedOrders.isEmpty()) {
                    orders.remove(symbol);
                }
            }

            onOrdersUpdated(order.getSymbol());

            if (isNewSymbol) {
                refreshCallback.runRefreshView(null);
            }
        }
    }

    private static boolean isSpread(final String symbol) {
        return symbol.contains("-");
    }

    private void onNewBook(final IBook<?> book) {
        if (rulesBySymbol.containsKey(book.getSymbol())) {
            if (createIfNewSymbol(book.getSymbol())) {
                refreshCallback.runRefreshView(null);
            }
        }
    }

    private boolean createIfNewSymbol(final String symbol) {

        final Set<SourcedWorkingOrder> ordersForSymbol = MapUtils.getMappedSet(orders, symbol);
        boolean isSymbolIsNew = ordersForSymbol.isEmpty();

        IBook<?> book = md.get(symbol);
        if (null == book) {
            book = bookSubscriber.subscribeToSymbol(symbol, this::onBookUpdated);
            if (null != book) {
                md.put(symbol, book);
                isSymbolIsNew = true;
            }
        }
        return isSymbolIsNew;
    }

    void addOrUpdateRule(final PullRule pullRule) {
        final AutoPullerEnabledPullRule enabledPullRule =
                rulesByID.computeIfAbsent(pullRule.ruleID, aLong -> new AutoPullerEnabledPullRule(pullRule));
        final PullRule prevRule = enabledPullRule.getPullRule();
        if (null != prevRule) {
            rulesBySymbol.remove(prevRule.symbol, enabledPullRule);
        }
        enabledPullRule.disable();
        enabledPullRule.setPullRule(pullRule);
        rulesBySymbol.put(enabledPullRule.getPullRule().symbol, enabledPullRule);
        persistence.updateRule(pullRule);
        if (createIfNewSymbol(pullRule.symbol)) {
            refreshCallback.runRefreshView(null);
        }
    }

    void deleteRule(final Long ruleID) {
        final AutoPullerEnabledPullRule enabledPullRule = rulesByID.remove(ruleID);
        if (null != enabledPullRule) {
            rulesBySymbol.remove(enabledPullRule.getPullRule().symbol, enabledPullRule);
            persistence.deleteRule(enabledPullRule.getPullRule());
        }
    }

    void enableRule(final String username, final Long ruleID) {
        final AutoPullerEnabledPullRule enabledPullRule = rulesByID.get(ruleID);
        if (null != enabledPullRule) {
            enabledPullRule.enable(username);
        }
    }

    AutoPullerEnabledPullRule disableRule(final Long ruleID) {
        final AutoPullerEnabledPullRule enabledPullRule = rulesByID.get(ruleID);
        if (null != enabledPullRule) {
            enabledPullRule.disable();
            return enabledPullRule;
        }
        return null;
    }

    Map<Long, AutoPullerEnabledPullRule> getRules() {
        return ImmutableMap.copyOf(rulesByID);
    }

    List<String> getRelevantSymbols() {
        final HashSet<String> symbols = new HashSet<>();
        symbols.addAll(orders.keySet());
        symbols.addAll(md.keySet());
        symbols.addAll(rulesBySymbol.keySet());
        return new ArrayList<>(symbols);
    }

    private void onBookUpdated(final IBook<?> book) {
        if (!md.containsKey(book.getSymbol())) {
            md.put(book.getSymbol(), book);
        }
        runSymbol(book.getSymbol());
    }

    private void onOrdersUpdated(final String symbol) {
        runSymbol(symbol);
    }

    private void runSymbol(final String symbol) {
        timeChecker();
        for (final AutoPullerEnabledPullRule pullRule : rulesBySymbol.get(symbol)) {
            final Set<SourcedWorkingOrder> workingOrders = orders.get(symbol);
            final IBook<?> book = md.get(symbol);
            if (pullRule.isEnabled() && null != book && null != workingOrders) {
                final List<RemoteOrderCommandToServer> cancels = pullRule.ordersToPull(workingOrders, book);
                cancels.forEach(commandPublisher::publish);
                if (!cancels.isEmpty()) {
                    System.out.println("---- Auto puller Fired --- " + new DateTime());
                    System.out.println("Rule:\n" + pullRule.pullRule);
                    System.out.println("Book:");
                    debugPrintMdBook(book);
                    System.out.println("Working orders: \n" + new ArrayList<>(workingOrders));
                    pullRule.disable();
                    refreshCallback.ruleFired(pullRule);
                }
            }
        }

    }

    private boolean checkTime() {
        return !new DateTime().isAfter(DISABLE_TIME);
    }

    List<Long> getMDPrices(final String symbol) {

        final TreeSet<Long> prices = new TreeSet<>(Comparator.reverseOrder());

        final Set<SourcedWorkingOrder> sourcedOrders = orders.get(symbol);
        addAllPrices(sourcedOrders, prices);

        for (final AutoPullerEnabledPullRule enabledPullRule : rulesBySymbol.get(symbol)) {
            prices.add(enabledPullRule.getPullRule().mktCondition.price);
            prices.add(enabledPullRule.getPullRule().orderSelection.fromPrice);
            prices.add(enabledPullRule.getPullRule().orderSelection.toPrice);
        }

        final IBook<?> book = md.get(symbol);
        if (null != book) {
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
                for (long price = prices.first(); price >= prices.last(); price = book.getTickTable().subtractTicks(price, 1)) {
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

    AutoPullerEnabledPullRule getRule(final long ruleID) {
        return rulesByID.get(ruleID);
    }

    void setCallbacks(final IAutoPullCallbacks callbacks) {
        this.refreshCallback = callbacks;
    }

    int getPullCount(final AutoPullerEnabledPullRule enabledPullRule) {
        final String symbol = enabledPullRule.pullRule.symbol;
        final Set<SourcedWorkingOrder> ordersForSymbol = orders.get(symbol);
        final IBook<?> book = md.get(symbol);
        if (null != book && null != ordersForSymbol) {
            final List<RemoteOrderCommandToServer> example = enabledPullRule.pullRule.ordersToPull("example", ordersForSymbol, book);
            return example.size();
        }
        return 0;
    }

    void disableAllRules() {
        rulesBySymbol.values().forEach(AutoPullerEnabledPullRule::disable);
    }

    public void timeChecker() {
        if (!checkTime()) {
            disableAllRules();
            refreshCallback.runRefreshView("Disabled for night-time");
        }
    }

    private void debugPrintMdBook(final IBook<?> book) {
        System.out.println("\t instid " + book.getInstID() + ", source " + book.getSourceExch() + " status " + book.getStatus());
        System.out.println("\t seqno " + book.getLastPacketSeqNum() + " reftime " + book.getReferenceNanoSinceMidnightUTC());
        System.out.println("\t valid " + book.isValid());
        debugPrintLevels(book.getBestBid());
        debugPrintLevels(book.getBestAsk());
    }

    private void debugPrintLevels(final IBookLevel bid) {
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
