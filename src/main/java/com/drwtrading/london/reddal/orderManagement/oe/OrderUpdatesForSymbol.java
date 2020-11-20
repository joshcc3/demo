package com.drwtrading.london.reddal.orderManagement.oe;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.google.common.collect.MapMaker;
import drw.eeif.eeifoe.OrderSide;

import java.util.Collection;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class OrderUpdatesForSymbol {

    private final String symbol;
    private final Map<BookSide, NavigableMap<Long, HashMap<String, UpdateFromServer>>> updatesBySideAndPrice =
            new EnumMap<>(BookSide.class);
    public Map<String, UpdateFromServer> updatesByKey = new HashMap<>();
    public Map<Long, Map<String, UpdateFromServer>> updatesByPrice = new MapMaker().makeComputingMap(price -> new HashMap<>());

    public OrderUpdatesForSymbol(final String symbol) {
        this.symbol = symbol;

        for (final BookSide side : BookSide.values()) {
            updatesBySideAndPrice.put(side, new TreeMap<>(Comparator.reverseOrder()));
        }
    }

    public void onUpdate(final UpdateFromServer update) {
        if (update.update.getOrder().getSymbol().equals(symbol)) {
            final String key = update.key;
            final BookSide side = convertSide(update.update.getOrder().getSide());
            updatesByKey.remove(key);
            updatesByPrice.values().forEach(map -> map.remove(key));
            updatesBySideAndPrice.get(side).values().forEach(map -> map.remove(key));
            if (!update.update.isDead()) {
                updatesByKey.put(key, update);
                final long indicativePrice = update.update.getIndicativePrice();
                updatesByPrice.get(indicativePrice).put(key, update);
                MapUtils.getMappedMap(updatesBySideAndPrice.get(side), indicativePrice).put(key, update);
            }
        }
    }

    public Collection<UpdateFromServer> getOrdersForPrice(final long price) {
        return updatesByPrice.get(price).values();
    }

    public NavigableMap<Long, HashMap<String, UpdateFromServer>> getOrdersInRange(final BookSide side, final long lowPrice,
            final long highPrice) {
        return updatesBySideAndPrice.get(side).subMap(highPrice, true, lowPrice, true);
    }

    public void onDisconnected(final ServerDisconnected disconnected) {
        updatesByKey.values().removeIf(fromServer -> fromServer.server.equals(disconnected.server));
        updatesByPrice.forEach((price, map) -> map.values().removeIf(fromServer -> fromServer.server.equals(disconnected.server)));
        updatesBySideAndPrice.values().forEach(byPrice -> byPrice.forEach(
                (price, byServer) -> byServer.values().removeIf(server -> server.server.equals(disconnected.server))));
    }

    private static BookSide convertSide(final OrderSide side) {
        if (side == OrderSide.BUY) {
            return BookSide.BID;
        } else {
            return BookSide.ASK;
        }
    }
}
