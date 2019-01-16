package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.utils.collections.MapUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;

public class WorkingOrdersByPrice {

    private final Map<SourcedWorkingOrder, Long> currentPricesByWorkingOrderID;
    private final Map<Long, LinkedHashSet<SourcedWorkingOrder>> ordersByPrice;

    public WorkingOrdersByPrice() {

        this.currentPricesByWorkingOrderID = new HashMap<>();
        this.ordersByPrice = new HashMap<>();
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
        final LinkedHashSet<SourcedWorkingOrder> workingOrders = MapUtils.getMappedLinkedSet(ordersByPrice, newPrice);
        workingOrders.add(workingOrder);
    }

    public void removeWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final long oldPrice = currentPricesByWorkingOrderID.remove(workingOrder);
        removeWorkingOrder(oldPrice, workingOrder);
    }

    private void removeWorkingOrder(final long price, final SourcedWorkingOrder workingOrder) {

        final LinkedHashSet<SourcedWorkingOrder> workingOrders = ordersByPrice.get(price);
        workingOrders.remove(workingOrder);
        if (workingOrders.isEmpty()) {
            ordersByPrice.remove(price);
        }
    }

    public boolean hasAnyWorkingOrder() {
        return !ordersByPrice.isEmpty();
    }

    public Collection<Long> getWorkingOrderPrices() {
        return ordersByPrice.keySet();
    }

    public boolean hasPriceLevel(final long price) {
        return ordersByPrice.containsKey(price);
    }

    public LinkedHashSet<SourcedWorkingOrder> getWorkingOrdersAtPrice(final long price) {
        return ordersByPrice.get(price);
    }

    public Collection<SourcedWorkingOrder> getAllWorkingOrders() {
        return currentPricesByWorkingOrderID.keySet();
    }
}
