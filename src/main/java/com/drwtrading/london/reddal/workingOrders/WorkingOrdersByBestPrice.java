package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.workingOrders.opxl.BestWorkingPriceForSymbol;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class WorkingOrdersByBestPrice {

    private final String symbol;

    private final Map<SourcedWorkingOrder, Long> currentPricesByWorkingOrderID;

    private final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> bidOrdersByPrice;
    private final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> askOrdersByPrice;

    public WorkingOrdersByBestPrice(final String symbol) {

        this.symbol = symbol;

        this.currentPricesByWorkingOrderID = new HashMap<>();

        this.bidOrdersByPrice = new TreeMap<>(Comparator.reverseOrder());
        this.askOrdersByPrice = new TreeMap<>();
    }

    public Long setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final long newPrice = workingOrder.order.getPrice();

        final Long oldPrice = currentPricesByWorkingOrderID.put(workingOrder, newPrice);

        if (null == oldPrice) {

            addWorkingOrder(workingOrder);

        } else if (oldPrice != newPrice) {

            removeWorkingOrder(oldPrice, workingOrder);
            addWorkingOrder(workingOrder);
        }

        return oldPrice;
    }

    private void addWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final long price = workingOrder.order.getPrice();

        final LinkedHashSet<SourcedWorkingOrder> workingOrders;
        if (BookSide.BID == workingOrder.order.getSide()) {
            workingOrders = MapUtils.getMappedLinkedSet(bidOrdersByPrice, price);
        } else {
            workingOrders = MapUtils.getMappedLinkedSet(askOrdersByPrice, price);
        }
        workingOrders.add(workingOrder);
    }

    public void removeWorkingOrder(final SourcedWorkingOrder workingOrder) {

        currentPricesByWorkingOrderID.remove(workingOrder);
        removeWorkingOrder(workingOrder.order.getPrice(), workingOrder);
    }

    private void removeWorkingOrder(final long price, final SourcedWorkingOrder workingOrder) {

        final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> ordersByPrice;
        if (BookSide.BID == workingOrder.order.getSide()) {
            ordersByPrice = bidOrdersByPrice;
        } else {
            ordersByPrice = askOrdersByPrice;
        }

        final LinkedHashSet<SourcedWorkingOrder> workingOrders = ordersByPrice.get(price);
        workingOrders.remove(workingOrder);
        if (workingOrders.isEmpty()) {
            ordersByPrice.remove(price);
        }
    }

    public boolean connectionLost(final String source) {

        return clearOrders(bidOrdersByPrice, source) || clearOrders(askOrdersByPrice, source);
    }

    private static boolean clearOrders(final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> orders, final String source) {

        boolean isSomethingDeleted = false;

        final Iterator<Map.Entry<Long, LinkedHashSet<SourcedWorkingOrder>>> priceLevelIterator = orders.entrySet().iterator();
        while (priceLevelIterator.hasNext()) {

            final Set<SourcedWorkingOrder> workingOrders = priceLevelIterator.next().getValue();
            final Iterator<SourcedWorkingOrder> workingOrderIterator = workingOrders.iterator();
            while (workingOrderIterator.hasNext()) {

                final SourcedWorkingOrder workingOrder = workingOrderIterator.next();
                if (source.equals(workingOrder.source)) {
                    workingOrderIterator.remove();
                    isSomethingDeleted = true;
                }
            }

            if (workingOrders.isEmpty()) {
                priceLevelIterator.remove();
            }
        }
        return isSomethingDeleted;
    }

    public boolean hasBestPrice(final BookSide side) {

        if (BookSide.BID == side) {
            return !bidOrdersByPrice.isEmpty();
        } else {
            return !askOrdersByPrice.isEmpty();
        }
    }

    public long getBestPrice(final BookSide side) {

        if (BookSide.BID == side) {
            return bidOrdersByPrice.firstKey();
        } else {
            return askOrdersByPrice.firstKey();
        }
    }

    public BestWorkingPriceForSymbol getBestWorkingPrices() {

        final Map.Entry<Long, LinkedHashSet<SourcedWorkingOrder>> topBidOrders = bidOrdersByPrice.firstEntry();
        final Map.Entry<Long, LinkedHashSet<SourcedWorkingOrder>> topAskOrders = askOrdersByPrice.firstEntry();

        final Long bidPrice;
        final Long bidQty;
        if (null == topBidOrders) {
            bidPrice = null;
            bidQty = null;
        } else {
            bidPrice = topBidOrders.getKey();
            bidQty = getTotalQty(topBidOrders.getValue());
        }

        final Long askPrice;
        final Long askQty;
        if (null == topAskOrders) {
            askPrice = null;
            askQty = null;
        } else {
            askPrice = topAskOrders.getKey();
            askQty = getTotalQty(topAskOrders.getValue());
        }

        return new BestWorkingPriceForSymbol(symbol, bidPrice, bidQty, askPrice, askQty);
    }

    private long getTotalQty(final Set<SourcedWorkingOrder> workingOrders) {

        long result = 0;
        for (final SourcedWorkingOrder workingOrder : workingOrders) {
            result += workingOrder.order.getOrderQty() - workingOrder.order.getFilledQty();
        }
        return result;
    }
}
