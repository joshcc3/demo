package com.drwtrading.london.reddal.workingOrders.obligations;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByPrice;
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

public class RFQObligationPresenter implements IRFQObligationPresenter {

    private static final RFQObligation DEFAULT = new RFQObligation("", Collections.singletonList(new RFQObligationValue(5e6, 10)));
    private static final List<RFQObligationValue> NOT_FOUND =
            Collections.singletonList(new RFQObligationValue(Double.POSITIVE_INFINITY, 10));

    private final FXCalc<?> fxCalc;
    private final Map<String, SearchResult> searchResults;
    private final Map<String, WorkingOrdersByPrice> orders;
    private final Map<String, SourcedWorkingOrder> sourcedOrders;

    private final WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    private final HashSet<String> changedSymbols = new HashSet<>();

    private final Predicate<String> symbolFilter;
    private RFQObligationSet rfqObligationMap = RFQObligationOPXL.EMPTY_OBLIGATIONS;

    public RFQObligationPresenter(final FXCalc<?> fxCalc, final Predicate<String> symbolFilter) {

        this.fxCalc = fxCalc;

        this.searchResults = new HashMap<>();
        this.orders = new HashMap<>();
        this.sourcedOrders = new HashMap<>();

        this.symbolFilter = symbolFilter;
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        sourcedOrders.put(sourcedOrder.uiKey, sourcedOrder);

        final WorkingOrdersByPrice orders = getWorkingOrderByPrice(sourcedOrder.order.getSymbol());
        orders.setWorkingOrder(sourcedOrder);
        changedSymbols.add(sourcedOrder.order.getSymbol());
    }

    private WorkingOrdersByPrice getWorkingOrderByPrice(final String symbol) {

        final WorkingOrdersByPrice result = orders.get(symbol);
        if (null == result) {
            final WorkingOrdersByPrice newWorkingOrders = new WorkingOrdersByPrice();
            orders.put(symbol, newWorkingOrders);
            return newWorkingOrders;
        } else {
            return result;
        }
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        sourcedOrders.put(sourcedOrder.uiKey, sourcedOrder);

        final WorkingOrdersByPrice result = orders.get(sourcedOrder.order.getSymbol());
        result.removeWorkingOrder(sourcedOrder);
    }

    @Override
    public void setNibblerDisconnected(final String source) {

        final Iterator<SourcedWorkingOrder> ordersIterator = sourcedOrders.values().iterator();

        while (ordersIterator.hasNext()) {
            final SourcedWorkingOrder order = ordersIterator.next();
            if (source.equals(order.source)) {
                deleteWorkingOrder(order);
                ordersIterator.remove();
            }
        }
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

    public void update() {
        for (final String changedSymbol : changedSymbols) {
            updateView(views.all(), changedSymbol);
        }
        changedSymbols.clear();
    }

    private void updateView(final View view, final String symbol) {

        final SearchResult searchResult = searchResults.get(symbol);
        final WorkingOrdersByPrice ordersForSymbol = orders.get(symbol);
        final RFQObligation obligation = rfqObligationMap.obligationMap.getOrDefault(symbol, DEFAULT);

        if (obligation.obligations.isEmpty()) {

            view.hide(symbol);

        } else if (null == searchResult || null == ordersForSymbol) {

            view.update(symbol, NOT_FOUND, NOT_FOUND);

        } else {

            final TreeMap<Double, Double> bids = new TreeMap<>(Comparator.reverseOrder());
            final TreeMap<Double, Double> asks = new TreeMap<>(Comparator.naturalOrder());

            final double fxRate = fxCalc.getLastValidMid(searchResult.instID.ccy, CCY.EUR);

            for (final Long price : ordersForSymbol.getWorkingOrderPrices()) {

                long bidQty = 0;
                long askQty = 0;

                for (final SourcedWorkingOrder sourcedOrder : ordersForSymbol.getWorkingOrdersAtPrice(price)) {

                    final long unfilledQty = sourcedOrder.order.getOrderQty() - sourcedOrder.order.getFilledQty();
                    switch (sourcedOrder.order.getSide()) {
                        case BID: {
                            bidQty += unfilledQty;
                            break;
                        }
                        case ASK: {
                            askQty += unfilledQty;
                            break;
                        }
                    }
                }

                final double dblPx = (double) price / Constants.NORMALISING_FACTOR;

                if (0 < bidQty) {
                    bids.put(dblPx, dblPx * bidQty * fxRate);
                }

                if (0 < askQty) {
                    asks.put(dblPx, dblPx * askQty * fxRate);
                }
            }

            if (bids.isEmpty() || asks.isEmpty()) {

                view.update(symbol, obligation.obligations, obligation.obligations);
            } else {
                final double tightestBid = bids.firstKey();
                final double tightestAsk = asks.firstKey();
                final double midPx = (tightestBid + tightestAsk) / 2.0;

                final List<RFQObligationValue> missedBids = computeMissedObligations(midPx, bids, obligation.obligations);
                final List<RFQObligationValue> missedAsks = computeMissedObligations(midPx, asks, obligation.obligations);

                if (missedBids.isEmpty() && missedAsks.isEmpty()) {
                    view.hide(symbol);
                } else {
                    view.update(symbol, missedBids, missedAsks);
                }
            }
        }
    }

    private static List<RFQObligationValue> computeMissedObligations(final double midPx, final TreeMap<Double, Double> pxToNotional,
            final List<RFQObligationValue> obligations) {

        final List<RFQObligationValue> missedObligations = new ArrayList<>();

        double aggregateNotional = 0.0;
        double lastPx = midPx;

        final Iterator<Map.Entry<Double, Double>> pxIter = pxToNotional.entrySet().iterator();
        for (final RFQObligationValue obligation : obligations) {

            while (aggregateNotional < obligation.notional && pxIter.hasNext()) {
                final Map.Entry<Double, Double> e = pxIter.next();
                final double px = e.getKey();
                final double notional = e.getValue();

                aggregateNotional += notional;
                lastPx = px;
            }

            final double bpsAwayFromMid = Math.abs(2e4 * (midPx - lastPx) / (midPx + lastPx));
            if (bpsAwayFromMid > obligation.bps) {
                missedObligations.add(new RFQObligationValue(obligation.notional, bpsAwayFromMid));
            } else if (aggregateNotional < obligation.notional) {
                missedObligations.add(new RFQObligationValue(obligation.notional, Double.POSITIVE_INFINITY));
            }

        }

        return missedObligations;
    }

    public interface View {

        void setNotionals(List<Double> notionals);

        void update(String symbol, List<RFQObligationValue> missedBid, List<RFQObligationValue> missedAsk);

        void hide(String symbol);
    }

}
