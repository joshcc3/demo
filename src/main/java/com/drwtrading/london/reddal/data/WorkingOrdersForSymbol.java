package com.drwtrading.london.reddal.data;

import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorkingOrdersForSymbol {

    public final String symbol;
    public final Map<String, WorkingOrderUpdateFromServer> ordersByKey = new LinkedHashMap<>();
    public final Multimap<Long, WorkingOrderUpdateFromServer> ordersByPrice = LinkedHashMultimap.create();

    public WorkingOrdersForSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public WorkingOrderUpdateFromServer onWorkingOrderUpdate(final WorkingOrderUpdateFromServer workingOrderUpdateFromServer) {
        final WorkingOrderUpdate workingOrderUpdate = workingOrderUpdateFromServer.value;
        if (workingOrderUpdate.getSymbol().equals(symbol)) {
            final WorkingOrderUpdateFromServer previous;
            if (workingOrderUpdate.getWorkingOrderState() == WorkingOrderState.DEAD) {
                previous = ordersByKey.remove(workingOrderUpdateFromServer.key());
            } else {
                previous = ordersByKey.put(workingOrderUpdateFromServer.key(), workingOrderUpdateFromServer);
                ordersByPrice.put(workingOrderUpdate.getPrice(), workingOrderUpdateFromServer);
            }
            // Need the .equals() because HashMultimap doesn't store duplicates, so we would delete the only
            // copy of this working order otherwise.
            if (previous != null && !workingOrderUpdateFromServer.equals(previous)) {
                ordersByPrice.remove(previous.value.getPrice(), previous);
            }
            return previous;
        } else {
            return null;
        }
    }

}
