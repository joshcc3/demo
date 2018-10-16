package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import eeif.execution.WorkingOrderState;
import eeif.execution.WorkingOrderUpdate;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public class WorkingOrdersForSymbol {

    private final String symbol;
    public final Map<String, WorkingOrderUpdateFromServer> ordersByKey;
    private final Map<Long, LinkedHashSet<WorkingOrderUpdateFromServer>> ordersByPrice;

    public WorkingOrdersForSymbol(final String symbol) {

        this.symbol = symbol;

        this.ordersByKey = new LinkedHashMap<>();
        this.ordersByPrice = new HashMap<>();
    }

    public WorkingOrderUpdateFromServer onWorkingOrderUpdate(final WorkingOrderUpdateFromServer workingOrderUpdateFromServer) {

        final WorkingOrderUpdate workingOrderUpdate = workingOrderUpdateFromServer.workingOrderUpdate;

        final WorkingOrderUpdateFromServer previous;
        if (workingOrderUpdate.getWorkingOrderState() == WorkingOrderState.DEAD) {
            previous = ordersByKey.remove(workingOrderUpdateFromServer.key());
        } else {
            previous = ordersByKey.put(workingOrderUpdateFromServer.key(), workingOrderUpdateFromServer);

            final long price = workingOrderUpdate.getPrice();
            final Set<WorkingOrderUpdateFromServer> workingOrders = MapUtils.getMappedLinkedSet(ordersByPrice, price);
            workingOrders.add(workingOrderUpdateFromServer);
        }

        if (null != previous && !workingOrderUpdateFromServer.equals(previous)) {
            final long prevPrice = previous.workingOrderUpdate.getPrice();
            final Set<WorkingOrderUpdateFromServer> workingOrders = ordersByPrice.get(prevPrice);
            workingOrders.remove(previous);
            if (workingOrders.isEmpty()) {
                ordersByPrice.remove(prevPrice);
            }
        }
        return previous;
    }

    public Collection<Long> getWorkingOrderPrices() {
        return ordersByPrice.keySet();
    }

    public Collection<WorkingOrderUpdateFromServer> getWorkingOrdersAtPrice(final long price) {

        final Collection<WorkingOrderUpdateFromServer> result = ordersByPrice.get(price);
        if (null == result) {
            return Collections.emptySet();
        } else {
            return result;
        }
    }

    public void onServerDisconnected(final WorkingOrderConnectionEstablished connectionEstablished) {
        for (final Iterator<WorkingOrderUpdateFromServer> it = ordersByKey.values().iterator(); it.hasNext(); ) {
            final WorkingOrderUpdateFromServer next = it.next();
            if (next.fromServer.equals(connectionEstablished.server)) {
                it.remove();
                ordersByPrice.remove(next.workingOrderUpdate.getPrice(), next);
            }
        }
    }
}
