package com.drwtrading.london.reddal.orderentry;

import com.drwtrading.london.reddal.util.UpdateFromServer;
import com.google.common.collect.MapMaker;

import java.util.HashMap;
import java.util.Map;

public class OrderUpdatesForSymbol {

    private final String symbol;

    public Map<String, UpdateFromServer> updatesByKey = new HashMap<>();
    public Map<Long, Map<String, UpdateFromServer>> updatesByPrice = new MapMaker().makeComputingMap(price -> new HashMap<>());

    public OrderUpdatesForSymbol(String symbol) {

        this.symbol = symbol;
    }

    public void onUpdate(UpdateFromServer update) {
        if (update.update.getOrder().getSymbol().equals(symbol)) {
            if (update.update.isDead()) {
                updatesByKey.remove(update.key());
                updatesByPrice.values().forEach(map -> map.remove(update.key()));
            } else {
                updatesByKey.put(update.key(), update);
                updatesByPrice.get(update.update.getIndicativePrice()).put(update.key(), update);
            }
        }
    }
}
