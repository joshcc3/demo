package com.drwtrading.london.reddal.obligations;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;

import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Predicate;

public class ObligationPresenter {

    private final FXCalc<?> fxCalc;
    private final HashMap<String, SearchResult> searchResults = new HashMap<>();
    private final HashMap<String, WorkingOrdersForSymbol> orders = new HashMap<>();

    private final WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    private final DecimalFormat format = new DecimalFormat("0.0");
    private final HashSet<String> changedSymbols = new HashSet<>();

    private final Predicate<String> symbolFilter;

    public ObligationPresenter(FXCalc<?> fxCalc, Predicate<String> symbolFilter) {
        this.fxCalc = fxCalc;
        this.symbolFilter = symbolFilter;
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {
        final View view = views.register(connected);
        for (final String symbol : searchResults.keySet()) {
            view.addContract(symbol);
            updateView(view,symbol);
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
    public void onSearchResult(SearchResult instrument) {
        if (symbolFilter.test(instrument.symbol)) {
            searchResults.put(instrument.symbol, instrument);
        }
    }

    public void onWorkingOrders(Map<String, WorkingOrderUpdateFromServer> workingOrderUpdateMap) {
        for (WorkingOrderUpdateFromServer update : workingOrderUpdateMap.values()) {
            final String symbol = update.workingOrderUpdate.getSymbol();
            if (symbolFilter.test(symbol)) {
                WorkingOrdersForSymbol ordersForSymbol = orders.computeIfAbsent(symbol, WorkingOrdersForSymbol::new);
                ordersForSymbol.onWorkingOrderUpdate(update);
                changedSymbols.add(symbol);
            }
        }
    }

    public void update() {
        for (String changedSymbol : changedSymbols) {
            updateView(views.all(), changedSymbol);
        }
        changedSymbols.clear();
    }

    public void updateView(View view, String symbol) {
        SearchResult searchResult = searchResults.get(symbol);
        WorkingOrdersForSymbol ordersForSymbol = orders.get(symbol);

        if (null == searchResult) {
            return;
        } else if (null == ordersForSymbol) {
            view.update(symbol, "Infinity", "0", "Infinity", "0");
        }

        final TreeMap<Double, Double> bids = new TreeMap<>(Comparator.reverseOrder());
        final TreeMap<Double, Double> asks = new TreeMap<>(Comparator.naturalOrder());

        final double fxRate = fxCalc.getLastValidMid(searchResult.instID.ccy, CCY.EUR);

        double totalBid = 0.0;
        double totalAsk = 0.0;
        
        for (Long price : ordersForSymbol.ordersByPrice.keySet()) {
            long bidQty = 0;
            long askQty = 0;

            for (WorkingOrderUpdateFromServer update : ordersForSymbol.ordersByPrice.get(price)) {
                int unfilledQty = update.workingOrderUpdate.getTotalQuantity()
                        - update.workingOrderUpdate.getFilledQuantity();
                switch (update.workingOrderUpdate.getSide()) {
                    case BID:
                        bidQty += unfilledQty;
                        break;
                    case OFFER:
                        askQty += unfilledQty;
                        break;
                }
            }

            double dblPx = (double) (price) / Constants.NORMALISING_FACTOR;

            if (bidQty > 0) {
                bids.put(dblPx, dblPx * bidQty * fxRate);
                totalBid += dblPx * bidQty * fxRate;
            }

            if (askQty > 0) {
                asks.put(dblPx, dblPx * askQty * fxRate);
                totalAsk += dblPx * askQty * fxRate;
            }
        }


        if (bids.isEmpty() || asks.isEmpty()) {
            view.update(symbol, "Infinity", "0", "Infinity", "0");
        }
        
        double tightestBid = bids.firstKey();
        double widestBid = bids.lastKey();

        double tightestAsk = asks.firstKey();
        double widestAsk = asks.lastKey();

        double tightestBps = 2e4 * (tightestAsk - tightestBid) / (tightestAsk + tightestBid);
        double widestBps = 2e4 * (widestAsk - widestBid) / (widestAsk + widestBid);

        double tightestEur = Math.min(bids.firstEntry().getValue(), asks.firstEntry().getValue());
        double maxEur = Math.min(totalBid, totalAsk);

        view.update(symbol, format.format(tightestBps), format.format(tightestEur),
                format.format(widestBps), format.format(maxEur));
    }

    public interface View {
        void addContract(String symbol);
        void update(String symbol, String bpsTop, String eurTop, String bpsMax, String eurMax);
    }

}
