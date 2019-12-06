package com.drwtrading.london.reddal.workingOrders.gtc;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrdersByUIKey;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class GTCWorkingOrderMaintainer implements IWorkingOrdersCallback {

    private final OPXLGTCWorkingOrdersPresenter opxlWriter;

    private final Map<String, SourcedWorkingOrdersByUIKey> workingOrders;

    public GTCWorkingOrderMaintainer(final OPXLGTCWorkingOrdersPresenter opxlWriter) {

        this.opxlWriter = opxlWriter;

        this.workingOrders = new HashMap<>();
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        if (OrderType.GTC == workingOrder.order.getOrderType()) {

            final String symbol = workingOrder.order.getSymbol();
            final SourcedWorkingOrdersByUIKey workingOrders =
                    this.workingOrders.computeIfAbsent(symbol, s -> new SourcedWorkingOrdersByUIKey());

            workingOrders.setWorkingOrder(workingOrder);
            setGTCCount(symbol, workingOrders);
        }
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder workingOrder) {

        if (OrderType.GTC == workingOrder.order.getOrderType()) {

            final String symbol = workingOrder.order.getSymbol();
            final SourcedWorkingOrdersByUIKey workingOrders = this.workingOrders.get(symbol);

            workingOrders.removeWorkingOrder(workingOrder);
            setGTCCount(symbol, workingOrders);
        }
    }

    @Override
    public void setNibblerDisconnected(final String source) {

        for (final Map.Entry<String, SourcedWorkingOrdersByUIKey> symbolOrders : workingOrders.entrySet()) {

            final SourcedWorkingOrdersByUIKey workingOrders = symbolOrders.getValue();

            if (workingOrders.clearNibblerOrders(source)) {
                final String symbol = symbolOrders.getKey();
                setGTCCount(symbol, workingOrders);
            }
        }
    }

    private void setGTCCount(final String symbol, final SourcedWorkingOrdersByUIKey workingOrders) {

        final int workingOrderCount = workingOrders.getWorkingOrders().size();
        final GTCWorkingOrderCount gtcCount = new GTCWorkingOrderCount(symbol, workingOrderCount);
        opxlWriter.setGTCWorkingOrderCount(gtcCount);
    }

    public Map<BookSide, Map<String, WorkingOrder>> getBestWorkingOrders() {

        final Map<String, WorkingOrder> bidOrders = new HashMap<>();
        final Map<String, WorkingOrder> askOrders = new HashMap<>();

        for (final Map.Entry<String, SourcedWorkingOrdersByUIKey> symbolWorkingOrders : workingOrders.entrySet()) {

            final SourcedWorkingOrdersByUIKey instWorkingOrders = symbolWorkingOrders.getValue();

            WorkingOrder bestBid = null;
            WorkingOrder bestAsk = null;

            for (final SourcedWorkingOrder sourcedOrder : instWorkingOrders.getWorkingOrders()) {

                final WorkingOrder workingOrder = sourcedOrder.order;
                if (BookSide.BID == workingOrder.getSide()) {

                    bestBid = getBestBidOrder(bestBid, workingOrder);
                } else {
                    bestAsk = getBestAskOrder(bestAsk, workingOrder);
                }
            }

            final String symbol = symbolWorkingOrders.getKey();
            if (null != bestBid) {
                bidOrders.put(symbol, bestBid);
            }
            if (null != bestAsk) {
                askOrders.put(symbol, bestAsk);
            }
        }

        final Map<BookSide, Map<String, WorkingOrder>> result = new EnumMap<>(BookSide.class);
        result.put(BookSide.BID, bidOrders);
        result.put(BookSide.ASK, askOrders);

        return result;
    }

    private static WorkingOrder getBestBidOrder(final WorkingOrder prevBest, final WorkingOrder contender) {

        if (null == prevBest || prevBest.getPrice() < contender.getPrice()) {
            return contender;
        } else {
            return prevBest;
        }
    }

    private static WorkingOrder getBestAskOrder(final WorkingOrder prevBest, final WorkingOrder contender) {

        if (null == prevBest || contender.getPrice() < prevBest.getPrice()) {
            return contender;
        } else {
            return prevBest;
        }
    }
}
