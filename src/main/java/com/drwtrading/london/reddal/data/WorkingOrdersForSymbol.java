package com.drwtrading.london.reddal.data;

import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.Map;

public class WorkingOrdersForSymbol {

    public final String symbol;
    public final Map<String, WorkingOrderUpdate> ordersByKey = new HashMap<String, WorkingOrderUpdate>();
    public final Multimap<Long, WorkingOrderUpdate> ordersByPrice = HashMultimap.create();

    public WorkingOrdersForSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void on(WorkingOrderUpdate workingOrderUpdate) {
        if (workingOrderUpdate.getSymbol().equals(symbol)) {
            String key = key(workingOrderUpdate);
            WorkingOrderUpdate previous;
            if (workingOrderUpdate.getWorkingOrderState() == WorkingOrderState.DEAD) {
                previous = ordersByKey.remove(key);
            } else {
                previous = ordersByKey.put(key, workingOrderUpdate);
                ordersByPrice.put(workingOrderUpdate.getPrice(), workingOrderUpdate);
            }
            if (previous != null) {
                ordersByPrice.remove(previous.getPrice(), previous);
            }
        }
    }

    public static String key(WorkingOrderUpdate workingOrderUpdate) {
        return workingOrderUpdate.getServerName() + ":" + workingOrderUpdate.getChainId();
    }
}
