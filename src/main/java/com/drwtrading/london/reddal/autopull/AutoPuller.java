package com.drwtrading.london.reddal.autopull;

import com.drwtrading.jetlang.autosubscribe.KeyedBatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevelWithOrders;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersPresenter;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;
import org.joda.time.DateTime;
import org.joda.time.Instant;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;

public class AutoPuller {

    private final Instant DISABLE_TIME = new DateTime().withHourOfDay(20).withMinuteOfHour(57).withSecondOfMinute(0).toInstant();

    private final Publisher<RemoteOrderCommandToServer> commandPublisher;
    private final PullerBookSubscriber bookSubscriber;
    private final Map<String, WorkingOrdersForSymbol> orders = new HashMap<>();
    private final Map<String, IBook<?>> md = new HashMap<>();
    private final Multimap<String, EnabledPullRule> rulesBySymbol = HashMultimap.create();
    private final Map<Long, EnabledPullRule> rulesByID = new TreeMap<>();
    private final AutoPullPersistence persistence;
    private IAutoPullCallbacks refreshCallback = IAutoPullCallbacks.DEFAULT;

    public AutoPuller(final Publisher<RemoteOrderCommandToServer> commandPublisher, final PullerBookSubscriber bookSubscriber,
            final AutoPullPersistence persistence) {
        this.commandPublisher = commandPublisher;
        this.bookSubscriber = bookSubscriber;
        this.persistence = persistence;
        bookSubscriber.setCreatedCallback(this::onNewBook);
        persistence.getPullRules().values().forEach(this::addOrUpdateRule);
    }

    @KeyedBatchSubscriber(converter = WorkingOrdersPresenter.WOConverter.class, flushInterval = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Subscribe
    public void on(final Map<String, WorkingOrderUpdateFromServer> woEvents) {

        final HashSet<String> updatedSymbols = new HashSet<>();
        boolean newSymbols = false;
        for (final WorkingOrderUpdateFromServer e : woEvents.values()) {

            final String symbol = e.workingOrderUpdate.getSymbol();

            if (e.isLikelyGTC() && isSpread(symbol)) {

                newSymbols |= createIfNewSymbol(symbol);

                orders.get(symbol).onWorkingOrderUpdate(e);
                updatedSymbols.add(symbol);
            }
        }

        updatedSymbols.forEach(this::onOrdersUpdated);
        if (newSymbols) {
            refreshCallback.runRefreshView(null);
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
        boolean symbolIsNew = false;
        WorkingOrdersForSymbol ordersForSymbol = orders.get(symbol);
        if (null == ordersForSymbol) {
            ordersForSymbol = new WorkingOrdersForSymbol(symbol);
            orders.put(symbol, ordersForSymbol);
            symbolIsNew = true;
        }

        IBook<?> book = md.get(symbol);
        if (null == book) {
            book = bookSubscriber.subscribeToSymbol(symbol, this::onBookUpdated);
            if (null != book) {
                md.put(symbol, book);
                symbolIsNew = true;
            }
        }
        return symbolIsNew;
    }

    void addOrUpdateRule(final PullRule pullRule) {
        final EnabledPullRule enabledPullRule = rulesByID.computeIfAbsent(pullRule.ruleID, aLong -> new EnabledPullRule(pullRule));
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
        final EnabledPullRule enabledPullRule = rulesByID.remove(ruleID);
        if (null != enabledPullRule) {
            rulesBySymbol.remove(enabledPullRule.getPullRule().symbol, enabledPullRule);
            persistence.deleteRule(enabledPullRule.getPullRule());
        }
    }

    void enableRule(final String username, final Long ruleID) {
        final EnabledPullRule enabledPullRule = rulesByID.get(ruleID);
        if (null != enabledPullRule) {
            enabledPullRule.enable(username);
        }
    }

    EnabledPullRule disableRule(final Long ruleID) {
        final EnabledPullRule enabledPullRule = rulesByID.get(ruleID);
        if (null != enabledPullRule) {
            enabledPullRule.disable();
            return enabledPullRule;
        }
        return null;
    }

    Map<Long, EnabledPullRule> getRules() {
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
        for (final EnabledPullRule pullRule : rulesBySymbol.get(symbol)) {
            final WorkingOrdersForSymbol workingOrdersForSymbol = orders.get(symbol);
            final IBook<?> book = md.get(symbol);
            if (pullRule.isEnabled() && null != book && null != workingOrdersForSymbol) {
                final List<RemoteOrderCommandToServer> cancels = pullRule.ordersToPull(workingOrdersForSymbol, book);
                cancels.forEach(commandPublisher::publish);
                if (!cancels.isEmpty()) {
                    System.out.println("---- Auto puller Fired --- " + new DateTime());
                    System.out.println("Rule:\n" + pullRule.pullRule);
                    System.out.println("Book:");
                    debugPrintMdBook(book);
                    System.out.println("Working orders: \n" + new ArrayList<>(workingOrdersForSymbol.ordersByKey.values()));
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

        final WorkingOrdersForSymbol workingOrdersForSymbol = orders.get(symbol);
        prices.addAll(workingOrdersForSymbol.getWorkingOrderPrices());

        for (final EnabledPullRule enabledPullRule : rulesBySymbol.get(symbol)) {
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

    EnabledPullRule getRule(final long ruleID) {
        return rulesByID.get(ruleID);
    }

    void setCallbacks(final IAutoPullCallbacks callbacks) {
        this.refreshCallback = callbacks;
    }

    int getPullCount(final EnabledPullRule enabledPullRule) {
        final String symbol = enabledPullRule.pullRule.symbol;
        final WorkingOrdersForSymbol ordersForSymbol = orders.get(symbol);
        final IBook<?> book = md.get(symbol);
        if (null != book && null != ordersForSymbol) {
            final List<RemoteOrderCommandToServer> example = enabledPullRule.pullRule.ordersToPull("example", ordersForSymbol, book);
            return example.size();
        }
        return 0;
    }

    void disableAllRules() {
        rulesBySymbol.values().forEach(EnabledPullRule::disable);
    }

    public void timeChecker() {
        if (!checkTime()) {
            disableAllRules();
            refreshCallback.runRefreshView("Disabled for night-time");
        }
    }

    static class EnabledPullRule {

        PullRule pullRule;
        String enabledByUser;

        EnabledPullRule(final PullRule pullRule) {
            this.pullRule = pullRule;
        }

        boolean isEnabled() {
            return null != enabledByUser;
        }

        void disable() {
            this.enabledByUser = null;
        }

        PullRule getPullRule() {
            return pullRule;
        }

        List<RemoteOrderCommandToServer> ordersToPull(final WorkingOrdersForSymbol workingOrders, final IBook<?> book) {
            if (isEnabled()) {
                return this.pullRule.ordersToPull(enabledByUser, workingOrders, book);
            } else {
                return Collections.emptyList();
            }
        }

        private void enable(final String user) {
            this.enabledByUser = user;
        }

        private void setPullRule(final PullRule pullRule) {
            this.pullRule = pullRule;
        }

        String getEnabledByUser() {
            return enabledByUser;
        }
    }

    public interface IAutoPullCallbacks {

        void runRefreshView(String message);

        void ruleFired(EnabledPullRule rule);

        IAutoPullCallbacks DEFAULT = new IAutoPullCallbacks() {
            @Override
            public void runRefreshView(final String message) {
            }

            @Override
            public void ruleFired(final EnabledPullRule rule) {
            }
        };
    }

    public void debugPrintMdBook(final IBook<?> book) {
        System.out.println("\t instid " + book.getInstID() + ", source " + book.getSourceExch() + " status " + book.getStatus());
        System.out.println("\t seqno " + book.getLastPacketSeqNum() + " reftime " + book.getReferenceNanoSinceMidnightUTC());
        System.out.println("\t valid " + book.isValid());
        debugPrintLevels(book.getBestBid());
        debugPrintLevels(book.getBestAsk());
    }

    public void debugPrintLevels(final IBookLevel bid) {
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
