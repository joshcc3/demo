package com.drwtrading.london.reddal.ladders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.util.Struct;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketClient;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.base.Function;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class OrdersPresenter {

    private final UILogger webLog;

    private final Publisher<SingleOrderCommand> singleOrderCommandPublisher;
    WebSocketViews<View> views = new WebSocketViews<>(View.class, this);
    Map<String, WorkingOrdersForSymbol> orders = new MapMaker().makeComputingMap(new Function<String, WorkingOrdersForSymbol>() {
        @Override
        public WorkingOrdersForSymbol apply(final String from) {
            return new WorkingOrdersForSymbol(from);
        }
    });
    Multimap<SymbolPrice, View> subscribed = HashMultimap.create();

    public OrdersPresenter(final UILogger webLog, final Publisher<SingleOrderCommand> singleOrderCommandPublisher) {

        this.webLog = webLog;
        this.singleOrderCommandPublisher = singleOrderCommandPublisher;
    }

    public void onWorkingOrderBatch(final List<WorkingOrderUpdateFromServer> batch) {
        batch.forEach(this::onWorkingOrder);
    }

    public void onWorkingOrder(final WorkingOrderUpdateFromServer update) {

        final String symbol = update.value.getSymbol();
        final WorkingOrdersForSymbol orders = this.orders.get(symbol);
        final WorkingOrderUpdateFromServer prevUpdate = orders.onWorkingOrderUpdate(update);
        final long newPrice = update.value.getPrice();
        final long prevPrice = null == prevUpdate ? update.value.getPrice() : prevUpdate.value.getPrice();
        update(symbol, newPrice, prevPrice);
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {
        views.register(connected);
    }

    @Subscribe
    public void on(final WebSocketDisconnected disconnected) {
        final View view = views.unregister(disconnected);
        for (final Iterator<Map.Entry<SymbolPrice, View>> it = subscribed.entries().iterator(); it.hasNext(); ) {
            final Map.Entry<SymbolPrice, View> next = it.next();
            if (next.getValue().equals(view)) {
                it.remove();
            }
        }
    }

    @Subscribe
    public void on(final WebSocketInboundData data) {
        webLog.write("ordersPresenter", data);
        views.invoke(data);
    }

    @FromWebSocketView
    public void subscribe(final String symbol, final String priceStr, final WebSocketInboundData data) {
        final View view = views.get(data.getOutboundChannel());
        final long price = Long.valueOf(priceStr);
        final SymbolPrice symbolPrice = new SymbolPrice(symbol, price);
        subscribed.put(symbolPrice, view);
        update(symbol, price, price);
    }

    @FromWebSocketView
    public void modifyQuantity(final String symbol, final String key, final int newRemainingQty, final WebSocketClient client) {
        singleOrderCommandPublisher.publish(new ModifyOrderQuantity(symbol, key, client.getUserName(), newRemainingQty));
    }

    @FromWebSocketView
    public void cancelOrder(final String symbol, final String key, final WebSocketClient client) {
        singleOrderCommandPublisher.publish(new CancelOrder(symbol, key, client.getUserName()));
    }

    private void update(final String symbol, final long newPrice, final long prevPrice) {

        final WorkingOrdersForSymbol workingOrders = this.orders.get(symbol);

        for (final Map.Entry<SymbolPrice, View> entry : subscribed.entries()) {

            final SymbolPrice symbolPrice = entry.getKey();
            final long price = symbolPrice.price;

            if (symbol.equals(symbolPrice.symbol) && (newPrice == price || prevPrice == price)) {

                final Collection<WorkingOrderUpdateFromServer> orders = workingOrders.ordersByPrice.get(price);
                entry.getValue().orders(orders);
            }
        }
    }

    public static interface View {

        void orders(Collection<WorkingOrderUpdateFromServer> workingOrderUpdates);
    }

    public static class SymbolPrice extends Struct {

        public final String symbol;
        public final long price;

        public SymbolPrice(final String symbol, final long price) {
            this.symbol = symbol;
            this.price = price;
        }
    }

    public static interface SingleOrderCommand {

        public String getSymbol();

        public String getOrderKey();

        public String getUsername();
    }

    public static class ModifyOrderQuantity extends Struct implements SingleOrderCommand {

        public final String symbol;

        public final String orderKey;
        public final String username;
        public final int newRemainingQuantity;

        public ModifyOrderQuantity(final String symbol, final String orderKey, final String username, final int newRemainingQuantity) {
            this.symbol = symbol;
            this.orderKey = orderKey;
            this.username = username;
            this.newRemainingQuantity = newRemainingQuantity;
        }

        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public String getOrderKey() {
            return orderKey;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }

    public static class CancelOrder extends Struct implements SingleOrderCommand {

        public final String symbol;
        public final String orderKey;
        public final String username;

        public CancelOrder(final String symbol, final String orderKey, final String username) {
            this.symbol = symbol;
            this.orderKey = orderKey;
            this.username = username;
        }

        @Override
        public String getSymbol() {
            return symbol;
        }

        @Override
        public String getOrderKey() {
            return orderKey;
        }

        @Override
        public String getUsername() {
            return username;
        }
    }

}
