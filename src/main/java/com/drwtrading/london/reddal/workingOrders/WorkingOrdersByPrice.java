package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class WorkingOrdersByPrice {

    private final Map<SourcedWorkingOrder, Long> currentPricesByWorkingOrderID;
    private final Map<Long, LinkedHashSet<SourcedWorkingOrder>> ordersByPrice;
    private final Map<BookSide, NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>>> ordersBySide = new EnumMap<>(BookSide.class);

    public WorkingOrdersByPrice() {

        this.currentPricesByWorkingOrderID = new HashMap<>();
        this.ordersByPrice = new HashMap<>();

        for (final BookSide side : BookSide.values()) {
            ordersBySide.put(side, new TreeMap<>(Comparator.reverseOrder()));
        }
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

        final long newPrice = workingOrder.order.getPrice();
        MapUtils.getMappedLinkedSet(ordersByPrice, newPrice).add(workingOrder);
        MapUtils.getMappedLinkedSet(ordersBySide.get(workingOrder.order.getSide()), newPrice).add(workingOrder);
    }

    public void removeWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final long oldPrice = currentPricesByWorkingOrderID.remove(workingOrder);
        removeWorkingOrder(oldPrice, workingOrder);
    }

    private void removeWorkingOrder(final long price, final SourcedWorkingOrder workingOrder) {
        removeWorkingOrder(price, workingOrder, ordersByPrice);
        removeWorkingOrder(price, workingOrder, ordersBySide.get(workingOrder.order.getSide()));
    }

    public boolean hasAnyWorkingOrder() {
        return !ordersByPrice.isEmpty();
    }

    public Collection<Long> getWorkingOrderPrices() {
        return ordersByPrice.keySet();
    }

    public NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> getOrdersInRange(final BookSide side, final long lowPrice,
            final long highPrice) {
        return ordersBySide.get(side).subMap(highPrice, true, lowPrice, true);
    }

    public boolean hasPriceLevel(final long price) {
        return ordersByPrice.containsKey(price);
    }

    public boolean hasOrderBetween(final long lowPrice, final long highPrice) {
        final Long bidBetween = ordersBySide.get(BookSide.BID).higherKey(highPrice + 1);
        final Long askBetween = ordersBySide.get(BookSide.ASK).higherKey(highPrice + 1);

        final boolean bidExists = bidBetween != null && bidBetween >= lowPrice;
        final boolean askExists = askBetween != null && askBetween >= lowPrice;
        return bidExists || askExists;
    }

    public LinkedHashSet<SourcedWorkingOrder> getWorkingOrdersAtPrice(final long price) {
        return ordersByPrice.get(price);
    }

    public Collection<SourcedWorkingOrder> getAllWorkingOrders() {
        return currentPricesByWorkingOrderID.keySet();
    }

    private static void removeWorkingOrder(final long price, final SourcedWorkingOrder workingOrder,
            final Map<Long, LinkedHashSet<SourcedWorkingOrder>> map) {
        final LinkedHashSet<SourcedWorkingOrder> workingOrders = map.get(price);
        workingOrders.remove(workingOrder);
        if (workingOrders.isEmpty()) {
            map.remove(price);
        }
    }
}
