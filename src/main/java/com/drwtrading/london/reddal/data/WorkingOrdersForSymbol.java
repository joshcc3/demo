package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.workingOrders.WorkingOrderConnectionEstablished;
import eeif.execution.WorkingOrderState;
import eeif.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.util.Iterator;
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
        final WorkingOrderUpdate workingOrderUpdate = workingOrderUpdateFromServer.workingOrderUpdate;
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
                ordersByPrice.remove(previous.workingOrderUpdate.getPrice(), previous);
            }
            return previous;
        } else {
            return null;
        }
    }

    public void onServerDisconnected(WorkingOrderConnectionEstablished connectionEstablished) {
        for (Iterator<WorkingOrderUpdateFromServer> it = ordersByKey.values().iterator(); it.hasNext(); ) {
            WorkingOrderUpdateFromServer next = it.next();
            if (next.fromServer.equals(connectionEstablished.server)) {
                it.remove();
                ordersByPrice.remove(next.workingOrderUpdate.getPrice(), next);
            }
        }
    }

}
