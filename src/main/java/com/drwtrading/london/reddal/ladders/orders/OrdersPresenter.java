package com.drwtrading.london.reddal.ladders.orders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import drw.eeif.eeifoe.Cancel;
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
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

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
        for (final Iterator<Map.Entry<OrdersPresenterSymbolPrice, IOrdersView>> it = subscribed.entries().iterator(); it.hasNext(); ) {
            final Map.Entry<OrdersPresenterSymbolPrice, IOrdersView> next = it.next();
            if (next.getValue().equals(view)) {
                it.remove();
            }
        }
    }

    @Subscribe
    public void on(final WebSocketInboundData data) {
        views.invoke(data);
    }

    @FromWebSocketView
    public void subscribe(final String symbol, final String priceStr, final WebSocketInboundData data) {
        final IOrdersView view = views.get(data.getOutboundChannel());
        final long price = Long.valueOf(priceStr);
        final OrdersPresenterSymbolPrice symbolPrice = new OrdersPresenterSymbolPrice(symbol, price);
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

            if (symbol.equals(symbolPrice.symbol) && (newPrice == price || prevPrice == price)) {
                final Collection<SourcedWorkingOrder> orders = workingOrders.getWorkingOrdersAtPrice(price);
                final Collection<Map<String, String>> data = getOrders(orders);
                entry.getValue().orders(data);
                entry.getValue().managedOrders(managed.getOrdersForPrice(price));
            }
        }
    }

    private static Collection<Map<String, String>> getOrders(final Collection<SourcedWorkingOrder> orders) {

        if (null == orders) {
            return Collections.emptySet();
        } else {
            final Collection<Map<String, String>> result = new HashSet<>();

            for (final SourcedWorkingOrder sourcedOrder : orders) {

                final Map<String, String> orderMap = new HashMap<>();
                result.add(orderMap);

                orderMap.put("id", sourcedOrder.uiKey);

                final WorkingOrder order = sourcedOrder.order;

                orderMap.put("side", order.getSide().name());
                orderMap.put("remainingQty", Long.toString(order.getOrderQty() - order.getFilledQty()));
                orderMap.put("type", order.getOrderType().name());
                orderMap.put("tag", order.getTag());
            }

            return result;
        }
    }
}
