package com.drwtrading.london.reddal.obligations;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderConnectionEstablished;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.Sets;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Predicate;

public class ObligationPresenter {

    private static final RFQObligation DEFAULT = new RFQObligation("", Collections.singletonList(new Obligation(5e6, 10)));
    private static final List<Obligation> NOT_FOUND = Collections.singletonList(new Obligation(Double.POSITIVE_INFINITY, 10));

    private final FXCalc<?> fxCalc;
    private final HashMap<String, SearchResult> searchResults = new HashMap<>();
    private final HashMap<String, WorkingOrdersForSymbol> orders = new HashMap<>();

    private final WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    private final HashSet<String> changedSymbols = new HashSet<>();

    private final Predicate<String> symbolFilter;
    private RFQObligationSet rfqObligationMap = ObligationOPXL.EMPTY_OBLIGATIONS;

    public ObligationPresenter(final FXCalc<?> fxCalc, final Predicate<String> symbolFilter) {
        this.fxCalc = fxCalc;
        this.symbolFilter = symbolFilter;
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {
        final View view = views.register(connected);
        if (null != rfqObligationMap) {
            refreshView(view);
        }
    }

    private void refreshView(final View view) {
        view.setNotionals(rfqObligationMap.notionals);
        for (final String symbol : Sets.union(searchResults.keySet(),
                Sets.union(rfqObligationMap.obligationMap.keySet(), orders.keySet()))) {
            updateView(view, symbol);
        }
    }

    @Subscribe
    public void on(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);

    }

    @Subscribe
    public void on(final WebSocketInboundData data) {
        // Ignore
    }

    @Subscribe
    public void onSearchResult(final SearchResult instrument) {
        if (symbolFilter.test(instrument.symbol)) {
            searchResults.put(instrument.symbol, instrument);
        }
    }

    public void updateObligations(final RFQObligationSet rfqObligationMap) {
        final boolean notionalsChanged =
                this.rfqObligationMap == null || !rfqObligationMap.notionals.equals(this.rfqObligationMap.notionals);
        this.rfqObligationMap = rfqObligationMap;
        changedSymbols.addAll(rfqObligationMap.obligationMap.keySet());
        if (notionalsChanged) {
            refreshView(views.all());
        }
    }

    public void onWorkingOrders(final Map<String, WorkingOrderUpdateFromServer> workingOrderUpdateMap) {
        for (final WorkingOrderUpdateFromServer update : workingOrderUpdateMap.values()) {
            final String symbol = update.workingOrderUpdate.getSymbol();
            if (symbolFilter.test(symbol)) {
                final WorkingOrdersForSymbol ordersForSymbol = orders.computeIfAbsent(symbol, WorkingOrdersForSymbol::new);
                ordersForSymbol.onWorkingOrderUpdate(update);
                changedSymbols.add(symbol);
            }
        }
    }

    public void onWorkingOrderConnected(final WorkingOrderConnectionEstablished connectionEstablished) {
        for (final WorkingOrdersForSymbol workingOrdersForSymbol : orders.values()) {
            workingOrdersForSymbol.onServerDisconnected(connectionEstablished);
        }

    }

    public void update() {
        for (final String changedSymbol : changedSymbols) {
            updateView(views.all(), changedSymbol);
        }
        changedSymbols.clear();
    }

    private void updateView(final View view, final String symbol) {

        final SearchResult searchResult = searchResults.get(symbol);
        final WorkingOrdersForSymbol ordersForSymbol = orders.get(symbol);
        final RFQObligation obligation = rfqObligationMap.obligationMap.getOrDefault(symbol, DEFAULT);

        if (obligation.obligations.isEmpty()) {
            view.hide(symbol);
            return;
        }

        if (null == searchResult || null == ordersForSymbol) {
            view.update(symbol, NOT_FOUND, NOT_FOUND);
            return;
        }

        final TreeMap<Double, Double> bids = new TreeMap<>(Comparator.reverseOrder());
        final TreeMap<Double, Double> asks = new TreeMap<>(Comparator.naturalOrder());

        final double fxRate = fxCalc.getLastValidMid(searchResult.instID.ccy, CCY.EUR);

        for (final Long price : ordersForSymbol.getWorkingOrderPrices()) {

            long bidQty = 0;
            long askQty = 0;

            for (final WorkingOrderUpdateFromServer update : ordersForSymbol.getWorkingOrdersAtPrice(price)) {
                final int unfilledQty = update.workingOrderUpdate.getTotalQuantity() - update.workingOrderUpdate.getFilledQuantity();
                switch (update.workingOrderUpdate.getSide()) {
                    case BID:
                        bidQty += unfilledQty;
                        break;
                    case OFFER:
                        askQty += unfilledQty;
                        break;
                }
            }

            final double dblPx = (double) price / Constants.NORMALISING_FACTOR;

            if (bidQty > 0) {
                bids.put(dblPx, dblPx * bidQty * fxRate);
            }

            if (askQty > 0) {
                asks.put(dblPx, dblPx * askQty * fxRate);
            }
        }

        if (bids.isEmpty() || asks.isEmpty()) {
            view.update(symbol, obligation.obligations, obligation.obligations);
            return;
        }

        final double tightestBid = bids.firstKey();
        final double tightestAsk = asks.firstKey();
        final double midPx = (tightestBid + tightestAsk) / 2.0;

        final List<Obligation> missedBids = computeMissedObligations(midPx, bids, obligation.obligations);
        final List<Obligation> missedAsks = computeMissedObligations(midPx, asks, obligation.obligations);

        if (missedBids.isEmpty() && missedAsks.isEmpty()) {
            view.hide(symbol);
        } else {
            view.update(symbol, missedBids, missedAsks);
        }
    }

    private List<Obligation> computeMissedObligations(final double midPx, final TreeMap<Double, Double> pxToNotional,
            final List<Obligation> obligations) {

        final List<Obligation> missedObligations = new ArrayList<>();

        double aggregateNotional = 0.0;
        double lastPx = midPx;

        final Iterator<Map.Entry<Double, Double>> pxIter = pxToNotional.entrySet().iterator();
        for (final Obligation obligation : obligations) {

            while (aggregateNotional < obligation.notional && pxIter.hasNext()) {
                final Map.Entry<Double, Double> e = pxIter.next();
                final double px = e.getKey();
                final double notional = e.getValue();

                aggregateNotional += notional;
                lastPx = px;
            }

            final double bpsAwayFromMid = Math.abs(2e4 * (midPx - lastPx) / (midPx + lastPx));
            if (bpsAwayFromMid > obligation.bps) {
                missedObligations.add(new Obligation(obligation.notional, bpsAwayFromMid));
            } else if (aggregateNotional < obligation.notional) {
                missedObligations.add(new Obligation(obligation.notional, Double.POSITIVE_INFINITY));
            }

        }

        return missedObligations;
    }

    public interface View {

        void setNotionals(List<Double> notionals);

        void update(String symbol, List<Obligation> missedBid, List<Obligation> missedAsk);

        void hide(String symbol);
    }

}
