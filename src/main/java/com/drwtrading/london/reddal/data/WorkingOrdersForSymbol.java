package com.drwtrading.london.reddal.data;

import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.Main;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.Map;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class WorkingOrdersForSymbol {

    public final String symbol;
    public final Map<String, Main.WorkingOrderUpdateFromServer> ordersByKey = newFastMap();
    public final Multimap<Long, Main.WorkingOrderUpdateFromServer> ordersByPrice = HashMultimap.create();

    public WorkingOrdersForSymbol(String symbol) {
        this.symbol = symbol;
    }

    public void onWorkingOrderUpdate(Main.WorkingOrderUpdateFromServer workingOrderUpdateFromServer) {
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
            if (!workingOrderUpdateFromServer.equals(previous)) {
                ordersByPrice.remove(previous.value.getPrice(), previous);
            }
        }
    }

}
