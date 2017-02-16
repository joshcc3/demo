package com.drwtrading.london.reddal.autopull;

import com.drwtrading.jetlang.autosubscribe.BatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class AutoPuller {

    private final Publisher<Main.RemoteOrderCommandToServer> commandPublisher;
    private final PullerBookSubscriber bookSubscriber;
    private final Map<String, WorkingOrdersForSymbol> orders = new HashMap<>();
    private final Map<String, IBook<?>> md = new HashMap<>();
    private final Multimap<String, EnabledPullRule> rulesBySymbol = HashMultimap.create();
    private final Map<Long, EnabledPullRule> rulesByID = new TreeMap<>();
    private final AutoPullPersistence persistence;
    private Runnable refreshCallback;

    public AutoPuller(Publisher<Main.RemoteOrderCommandToServer> commandPublisher, PullerBookSubscriber bookSubscriber, AutoPullPersistence persistence) {
        this.commandPublisher = commandPublisher;
        this.bookSubscriber = bookSubscriber;
        this.persistence = persistence;
        bookSubscriber.setCreatedCallback(this::onNewBook);
        persistence.getPullRules().values().forEach(this::addOrUpdateRule);
    }

    @BatchSubscriber
    @Subscribe
    public void on(List<WorkingOrderUpdateFromServer> woEvents) {
        HashSet<String> updatedSymbols = new HashSet<>();
        boolean newSymbols = false;
        for (WorkingOrderUpdateFromServer e : woEvents) {
            if (!e.isLikelyGTC()) {
                continue;
            }

            String symbol = e.value.getSymbol();
            newSymbols |= createIfNewSymbol(symbol);

            orders.get(symbol).onWorkingOrderUpdate(e);
            updatedSymbols.add(symbol);
        }

        updatedSymbols.forEach(this::onOrdersUpdated);
        if (newSymbols) {
            triggerRefresh();
        }

    }

    private void onNewBook(IBook<?> book) {
        if (rulesBySymbol.containsKey(book.getSymbol())) {
            if (createIfNewSymbol(book.getSymbol())) {
                triggerRefresh();
            }
        }
    }

    private boolean createIfNewSymbol(String symbol) {
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

    public void addOrUpdateRule(PullRule pullRule) {
        EnabledPullRule enabledPullRule = rulesByID.computeIfAbsent(pullRule.ruleID, aLong -> new EnabledPullRule(pullRule));
        PullRule prevRule = enabledPullRule.getPullRule();
        if (null != prevRule) {
            rulesBySymbol.remove(prevRule.symbol, enabledPullRule);
        }
        enabledPullRule.disable();
        enabledPullRule.setPullRule(pullRule);
        rulesBySymbol.put(enabledPullRule.getPullRule().symbol, enabledPullRule);
        persistence.updateRule(pullRule);
        if (createIfNewSymbol(pullRule.symbol)) {
            triggerRefresh();
        }
    }

    public void deleteRule(Long ruleID) {
        EnabledPullRule enabledPullRule = rulesByID.remove(ruleID);
        if (null != enabledPullRule) {
            rulesBySymbol.remove(enabledPullRule.getPullRule().symbol, enabledPullRule);
            persistence.deleteRule(enabledPullRule.getPullRule());
        }
    }

    public EnabledPullRule enableRule(String username, Long ruleID) {
        EnabledPullRule enabledPullRule = rulesByID.get(ruleID);
        if (null != enabledPullRule) {
            enabledPullRule.enable(username);
            return enabledPullRule;
        }
        return null;
    }

    public EnabledPullRule disableRule(Long ruleID) {
        EnabledPullRule enabledPullRule = rulesByID.get(ruleID);
        if (null != enabledPullRule) {
            enabledPullRule.disable();
            return enabledPullRule;
        }
        return null;
    }

    public Map<Long, EnabledPullRule> getRules() {
        return ImmutableMap.copyOf(rulesByID);
    }

    public List<String> getRelevantSymbols() {
        HashSet<String> symbols = new HashSet<>();
        symbols.addAll(orders.keySet());
        symbols.addAll(md.keySet());
        symbols.addAll(rulesBySymbol.keySet());
        return new ArrayList<>(symbols);
    }

    public void onBookUpdated(IBook<?> book) {
        if (!md.containsKey(book.getSymbol())) {
            md.put(book.getSymbol(), book);
        }
        runSymbol(book.getSymbol());
    }

    public void onOrdersUpdated(String symbol) {
        runSymbol(symbol);
    }

    public void runSymbol(String symbol) {
        boolean pulledSomething = false;
        for (EnabledPullRule pullRule : rulesBySymbol.get(symbol)) {
            WorkingOrdersForSymbol workingOrdersForSymbol = orders.get(symbol);
            IBook<?> book = md.get(symbol);
            if (pullRule.isEnabled() && null != book && null != workingOrdersForSymbol) {
                List<Main.RemoteOrderCommandToServer> cancels = pullRule.ordersToPull(workingOrdersForSymbol, book);
                cancels.forEach(commandPublisher::publish);
                if (!cancels.isEmpty()) {
                    pullRule.disable();
                    pulledSomething = true;
                }
            }
        }
        if (pulledSomething) {
            triggerRefresh();
        }
    }

    public void triggerRefresh() {
        if (null != refreshCallback) {
            refreshCallback.run();
        }
    }

    public List<Long> getMDPrices(String symbol) {
        TreeSet<Long> prices = new TreeSet<>(Comparator.reverseOrder());

        WorkingOrdersForSymbol workingOrdersForSymbol = orders.get(symbol);
        prices.addAll(workingOrdersForSymbol.ordersByPrice.keySet());

        for (EnabledPullRule enabledPullRule : rulesBySymbol.get(symbol)) {
            prices.add(enabledPullRule.getPullRule().mktCondition.price);
            prices.add(enabledPullRule.getPullRule().orderSelection.fromPrice);
            prices.add(enabledPullRule.getPullRule().orderSelection.toPrice);
        }

        IBook<?> book = md.get(symbol);
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

    public EnabledPullRule getRule(long ruleID) {
        return rulesByID.get(ruleID);
    }

    public void setRefreshCallback(Runnable refreshCallback) {
        this.refreshCallback = refreshCallback;
    }

    public int getPullCount(EnabledPullRule enabledPullRule) {
        String symbol = enabledPullRule.pullRule.symbol;
        WorkingOrdersForSymbol ordersForSymbol = orders.get(symbol);
        IBook<?> book = md.get(symbol);
        if (null != book && null != ordersForSymbol) {
            List<Main.RemoteOrderCommandToServer> example = enabledPullRule.pullRule.ordersToPull("example", ordersForSymbol, book);
            return example.size();
        }
        return 0;
    }

    public void disableAllRules() {
        rulesBySymbol.values().forEach(EnabledPullRule::disable);
    }

    public static class EnabledPullRule {

        PullRule pullRule;
        String enabledByUser;

        EnabledPullRule(PullRule pullRule) {
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

        List<Main.RemoteOrderCommandToServer> ordersToPull(WorkingOrdersForSymbol workingOrders, IBook<?> book) {
            if (isEnabled()) {
                return this.pullRule.ordersToPull(enabledByUser, workingOrders, book);
            } else {
                return Collections.emptyList();
            }
        }

        private void enable(String user) {
            this.enabledByUser = user;
        }

        private void setPullRule(PullRule pullRule) {
            this.pullRule = pullRule;
        }

        public String getEnabledByUser() {
            return enabledByUser;
        }
    }


}
