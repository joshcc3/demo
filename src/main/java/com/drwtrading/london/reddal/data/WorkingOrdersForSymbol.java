package com.drwtrading.london.reddal.data;

import com.drwtrading.london.protocols.photon.execution.WorkingOrderState;
import com.drwtrading.london.protocols.photon.execution.WorkingOrderUpdate;
import com.drwtrading.london.reddal.Main;
import com.drwtrading.london.reddal.TradingStatusWatchdog;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class WorkingOrdersForSymbol {

    public final String symbol;
    public final Map<String, Main.WorkingOrderUpdateFromServer> ordersByKey = newFastMap();
    public final Map<String, TradingStatusWatchdog.ServerTradingStatus> tradingStatusByServer = newFastMap();
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
            if (previous != null) {
                ordersByPrice.remove(previous.value.getPrice(), previous);
            }
        }
    }

    public void onTradingStatus(TradingStatusWatchdog.ServerTradingStatus serverTradingStatus) {
        // Delete all orders from this server if the working order status is not OK
        for (Iterator<Main.WorkingOrderUpdateFromServer> iter = ordersByKey.values().iterator(); iter.hasNext(); ) {
            Main.WorkingOrderUpdateFromServer order = iter.next();
            if (order.fromServer.equals(serverTradingStatus.server) && serverTradingStatus.workingOrderStatus != Main.Status.OK) {
                iter.remove();
                ordersByPrice.remove(order.value.getPrice(), order);
            }
        }

        tradingStatusByServer.put(serverTradingStatus.server, serverTradingStatus);
    }

}
