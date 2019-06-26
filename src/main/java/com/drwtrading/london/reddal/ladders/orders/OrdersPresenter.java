package com.drwtrading.london.reddal.ladders.orders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.ladders.CancelOrderCmd;
import com.drwtrading.london.reddal.ladders.IOrdersView;
import com.drwtrading.london.reddal.ladders.ISingleOrderCommand;
import com.drwtrading.london.reddal.ladders.ModifyOrderQtyCmd;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryFromServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderUpdatesForSymbol;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByPrice;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketClient;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import drw.eeif.eeifoe.Cancel;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;

public class OrdersPresenter {

    private final Publisher<ISingleOrderCommand> singleOrderCommandPublisher;
    private final Publisher<OrderEntryCommandToServer> orderEntryCommandToServer;
    private final WebSocketViews<IOrdersView> views = new WebSocketViews<>(IOrdersView.class, this);

    private final Map<String, WorkingOrdersByPrice> orders = new MapMaker().makeComputingMap(s -> new WorkingOrdersByPrice());
    private final Map<String, OrderUpdatesForSymbol> managedOrders = new MapMaker().makeComputingMap(OrderUpdatesForSymbol::new);
    private final Multimap<OrdersPresenterSymbolPrice, IOrdersView> subscribed = HashMultimap.create();

    public OrdersPresenter(final Publisher<ISingleOrderCommand> singleOrderCommandPublisher,
            final Publisher<OrderEntryCommandToServer> orderEntryCommandToServer) {
        this.singleOrderCommandPublisher = singleOrderCommandPublisher;
        this.orderEntryCommandToServer = orderEntryCommandToServer;
    }

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final String symbol = sourcedOrder.order.getSymbol();
        final long newPrice = sourcedOrder.order.getPrice();

        final WorkingOrdersByPrice orders = this.orders.get(symbol);
        final Long prevPrice = orders.setWorkingOrder(sourcedOrder);

        if (null == prevPrice) {
            update(symbol, newPrice, newPrice);
        } else {
            update(symbol, sourcedOrder.order.getPrice(), prevPrice);
        }
    }

    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final String symbol = sourcedOrder.order.getSymbol();
        final long oldPrice = sourcedOrder.order.getPrice();

        final WorkingOrdersByPrice orders = this.orders.get(symbol);
        orders.removeWorkingOrder(sourcedOrder);

        update(symbol, oldPrice, oldPrice);
    }

    public void setOrderEntryUpdate(final OrderEntryFromServer update) {

        if (update instanceof UpdateFromServer) {
            final UpdateFromServer updateFromServer = (UpdateFromServer) update;
            final OrderUpdatesForSymbol symbolUpdates = managedOrders.get(updateFromServer.symbol);
            symbolUpdates.onUpdate(updateFromServer);
        } else if (update instanceof ServerDisconnected) {
            oeServerDisconnected((ServerDisconnected) update);
        }
    }

    private void oeServerDisconnected(final ServerDisconnected serverDisconnected) {
        managedOrders.values().forEach(orderUpdatesForSymbol -> orderUpdatesForSymbol.onDisconnected(serverDisconnected));
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {
        views.register(connected);
    }

    @Subscribe
    public void on(final WebSocketDisconnected disconnected) {
        final IOrdersView view = views.unregister(disconnected);
        subscribed.entries().removeIf(next -> next.getValue().equals(view));
    }

    @Subscribe
    public void on(final WebSocketInboundData data) {
        views.invoke(data);
    }

    @FromWebSocketView
    public void subscribe(final String symbol, final String priceStr, final String bidPriceStr, final String askPriceStr,
            final WebSocketInboundData data) {
        final IOrdersView view = views.get(data.getOutboundChannel());
        final long price = Long.valueOf(priceStr);
        final long bidPrice = Long.valueOf(bidPriceStr);
        final long askPrice = Long.valueOf(askPriceStr);
        final OrdersPresenterSymbolPrice symbolPrice = new OrdersPresenterSymbolPrice(symbol, price, bidPrice, askPrice);
        subscribed.put(symbolPrice, view);
        update(symbol, price, price);
    }

    @FromWebSocketView
    public void modifyQuantity(final String symbol, final String key, final int newRemainingQty, final WebSocketClient client) {
        singleOrderCommandPublisher.publish(new ModifyOrderQtyCmd(symbol, key, client.getUserName(), newRemainingQty));
    }

    @FromWebSocketView
    public void cancelOrder(final String symbol, final String key, final WebSocketClient client) {
        singleOrderCommandPublisher.publish(new CancelOrderCmd(symbol, key, client.getUserName()));
    }

    @FromWebSocketView
    public void cancelManagedOrder(final String symbol, final String key, final WebSocketClient client) {
        final UpdateFromServer updateFromServer = managedOrders.get(symbol).updatesByKey.get(key);
        orderEntryCommandToServer.publish(new OrderEntryCommandToServer(updateFromServer.server,
                new Cancel(updateFromServer.update.getSystemOrderId(), updateFromServer.update.getOrder())));
    }

    private void update(final String symbol, final long newPrice, final long prevPrice) {

        final WorkingOrdersByPrice workingOrders = this.orders.get(symbol);
        final OrderUpdatesForSymbol managed = this.managedOrders.get(symbol);

        for (final Map.Entry<OrdersPresenterSymbolPrice, IOrdersView> entry : subscribed.entries()) {

            final OrdersPresenterSymbolPrice symbolPrice = entry.getKey();
            final long price = symbolPrice.price;
            final long bidPrice = symbolPrice.bidPrice;
            final long askPrice = symbolPrice.askPrice;
            final boolean newPriceInRange = askPrice <= newPrice && newPrice <= bidPrice;
            final boolean prevPriceInRange = askPrice <= prevPrice && prevPrice <= bidPrice;

            if (symbol.equals(symbolPrice.symbol) && (newPriceInRange || prevPriceInRange)) {
                final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> bidOrders =
                        workingOrders.getOrdersInRange(BookSide.BID, price, bidPrice);
                final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> askOrders =
                        workingOrders.getOrdersInRange(BookSide.ASK, askPrice, price);
                final Collection<Map<String, String>> data = getOrders(bidOrders, askOrders);
                entry.getValue().orders(data);
                final NavigableMap<Long, HashMap<String, UpdateFromServer>> bidUpdates =
                        managed.getOrdersInRange(BookSide.BID, price, bidPrice);
                final NavigableMap<Long, HashMap<String, UpdateFromServer>> askUpdates =
                        managed.getOrdersInRange(BookSide.ASK, askPrice, price);
                entry.getValue().managedOrders(collectUpdates(bidUpdates, askUpdates));
            }
        }
    }

    private static Collection<UpdateFromServer> collectUpdates(final NavigableMap<Long, HashMap<String, UpdateFromServer>> bidUpdates,
            final NavigableMap<Long, HashMap<String, UpdateFromServer>> askUpdates) {
        if (bidUpdates.isEmpty() && askUpdates.isEmpty()) {
            return Collections.emptySet();
        } else {
            final Collection<UpdateFromServer> result = new ArrayList<>();
            bidUpdates.values().forEach(updates -> result.addAll(updates.values()));
            askUpdates.values().forEach(updates -> result.addAll(updates.values()));

            return result;
        }
    }

    private static Collection<Map<String, String>> getOrders(final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> bidOrders,
            final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> askOrders) {

        if (bidOrders.isEmpty() && askOrders.isEmpty()) {
            return Collections.emptySet();
        } else {
            final Collection<Map<String, String>> result = new HashSet<>();
            addOrders(bidOrders, result);
            addOrders(askOrders, result);

            return result;
        }
    }

    private static void addOrders(final NavigableMap<Long, LinkedHashSet<SourcedWorkingOrder>> ordersByPrice,
            final Collection<Map<String, String>> result) {
        for (final LinkedHashSet<SourcedWorkingOrder> orders : ordersByPrice.values()) {
            for (final SourcedWorkingOrder sourcedOrder : orders) {

                final Map<String, String> orderMap = new HashMap<>();
                result.add(orderMap);

                orderMap.put("id", sourcedOrder.uiKey);

                final WorkingOrder order = sourcedOrder.order;

                orderMap.put("side", order.getSide().name());
                orderMap.put("remainingQty", Long.toString(order.getOrderQty() - order.getFilledQty()));
                orderMap.put("type", order.getOrderType().name());
                orderMap.put("tag", order.getTag());
                orderMap.put("price", Double.toString((double) order.getPrice() / Constants.NORMALISING_FACTOR));
            }
        }
    }
}
