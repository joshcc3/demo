package com.drwtrading.london.reddal.data;

import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.Main;
import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;

import java.util.LinkedHashMap;
import java.util.Map;

public class WorkingOrdersForSymbol {

    public final String symbol;
    public final Map<String, Main.WorkingOrderUpdateFromServer> ordersByKey = new LinkedHashMap<>();
    public final Multimap<Long, Main.WorkingOrderUpdateFromServer> ordersByPrice = LinkedHashMultimap.create();
    public WorkingOrdersForSymbol(String symbol) {
        this.symbol = symbol;
    }

    public Main.WorkingOrderUpdateFromServer onWorkingOrderUpdate(Main.WorkingOrderUpdateFromServer workingOrderUpdateFromServer) {
        WorkingOrderUpdate workingOrderUpdate = workingOrderUpdateFromServer.value;
        if (workingOrderUpdate.getSymbol().equals(symbol)) {
            Main.WorkingOrderUpdateFromServer previous;
            if (workingOrderUpdate.getWorkingOrderState() == WorkingOrderState.DEAD) {
                previous = ordersByKey.remove(workingOrderUpdateFromServer.key());
            } else {
                previous = ordersByKey.put(workingOrderUpdateFromServer.key(), workingOrderUpdateFromServer);
                ordersByPrice.put(workingOrderUpdate.getPrice(), workingOrderUpdateFromServer);
            }
            // Need the .equals() because HashMultimap doesn't store duplicates, so we would delete the only
            // copy of this working order otherwise.
            if (previous != null && !workingOrderUpdateFromServer.equals(previous) ) {
                ordersByPrice.remove(previous.value.getPrice(), previous);
            }
            return previous;
        } else {
            return null;
        }
    }

}
