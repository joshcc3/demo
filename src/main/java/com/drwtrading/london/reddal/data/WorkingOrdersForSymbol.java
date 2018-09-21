package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderConnectionEstablished;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
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

    public final String symbol;
    public final Map<String, WorkingOrderUpdateFromServer> ordersByKey;
    private final Map<Long, LinkedHashSet<WorkingOrderUpdateFromServer>> ordersByPrice;

    public WorkingOrdersForSymbol(final String symbol) {

        this.symbol = symbol;

        this.ordersByKey = new LinkedHashMap<>();
        this.ordersByPrice = new HashMap<>();
    }

    public WorkingOrderUpdateFromServer onWorkingOrderUpdate(final WorkingOrderUpdateFromServer workingOrderUpdateFromServer) {

        final WorkingOrderUpdate workingOrderUpdate = workingOrderUpdateFromServer.workingOrderUpdate;

        if (workingOrderUpdate.getSymbol().equals(symbol)) {

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
        } else {
            return null;
        }
    }

    public Collection<Long> getWorkingOrderPrices() {
        return ordersByPrice.keySet();
    }

    public boolean hasPriceLevel(final long price) {

        return ordersByPrice.containsKey(price);
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

    public void removeOrdersFromServer(final String server) {

        final Iterator<LinkedHashSet<WorkingOrderUpdateFromServer>> iter = ordersByPrice.values().iterator();
        while (iter.hasNext()) {

            final LinkedHashSet<WorkingOrderUpdateFromServer> priceLevel = iter.next();

            final Iterator<WorkingOrderUpdateFromServer> workingOrderIter = priceLevel.iterator();
            while (workingOrderIter.hasNext()) {

                final WorkingOrderUpdateFromServer working = workingOrderIter.next();
                if (working.fromServer.equals(server)) {
                    iter.remove();
                }
            }
            if (priceLevel.isEmpty()) {
                iter.remove();
            }
        }
    }
}
