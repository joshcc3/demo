package com.drwtrading.london.reddal.orderentry;

import com.google.common.collect.MapMaker;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class OrderUpdatesForSymbol {

    private final String symbol;

    public Map<String, UpdateFromServer> updatesByKey = new HashMap<>();
    public Map<Long, Map<String, UpdateFromServer>> updatesByPrice = new MapMaker().makeComputingMap(price -> new HashMap<>());

    public OrderUpdatesForSymbol(final String symbol) {

        this.symbol = symbol;
    }

    public void onUpdate(final UpdateFromServer update) {
        if (update.update.getOrder().getSymbol().equals(symbol)) {
            updatesByKey.remove(update.key);
            updatesByPrice.values().forEach(map -> map.remove(update.key));
            if (!update.update.isDead()) {
                updatesByKey.put(update.key, update);
                updatesByPrice.get(update.update.getIndicativePrice()).put(update.key, update);
            }
        }
    }

    public Collection<UpdateFromServer> getOrdersForPrice(long price) {
        return updatesByPrice.get(price).values();
    }

    public void onDisconnected(final ServerDisconnected disconnected) {
        for (final Iterator<UpdateFromServer> iterator = updatesByKey.values().iterator(); iterator.hasNext(); ) {
            final UpdateFromServer fromServer = iterator.next();
            if (fromServer.server.equals(disconnected.server)) {
                iterator.remove();
            }
        }
        updatesByPrice.forEach((aLong, map) -> {
            for (final Iterator<UpdateFromServer> iterator = map.values().iterator(); iterator.hasNext(); ) {
                final UpdateFromServer fromServer = iterator.next();
                if (fromServer.server.equals(disconnected.server)) {
                    iterator.remove();
                }
            }
        });
    }

}
