package com.drwtrading.london.reddal.shredders;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.safety.ServerTradingStatus;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.MapMaker;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class ShredderPresenter {

    private static final long BATCH_FLUSH_INTERVAL_MS = 1000 / 5;
    private static final long HEARTBEAT_INTERVAL_MS = 1000;

    private final Map<Publisher<WebSocketOutboundData>, ShredderView> viewsBySocket = new HashMap<>();
    private final Multimap<String, ShredderView> viewsBySymbol = HashMultimap.create();
    private final Multimap<String, ShredderView> viewsByUser = HashMultimap.create();
    private final Map<String, MDForSymbol> marketDataForSymbolMap;
    private final IMDSubscriber bookSubscriber;
    private final Map<String, WorkingOrdersForSymbol> ordersBySymbol = new MapMaker().makeComputingMap(WorkingOrdersForSymbol::new);

    public ShredderPresenter(final IMDSubscriber depthBookSubscriber) {
        this.bookSubscriber = depthBookSubscriber;
        marketDataForSymbolMap = new MapMaker().makeComputingMap(this::subscribeToMarketDataForSymbol);
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        final UiPipeImpl uiPipe = new UiPipeImpl(connected.getOutboundChannel());
        final IShredderUI view = new WebSocketOutputDispatcher<>(IShredderUI.class).wrap(msg -> uiPipe.eval(msg.getData()));
        final ShredderView shredderView = new ShredderView(view, uiPipe);

        viewsBySocket.put(connected.getOutboundChannel(), shredderView);
        viewsByUser.put(connected.getClient().getUserName(), shredderView);
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {
        final ShredderView view = viewsBySocket.remove(disconnected.getOutboundChannel());
        if (view != null && view.symbol != null) {
            final String symbol = view.symbol;
            viewsBySymbol.remove(symbol, view);
            final String user = disconnected.getClient().getUserName();
            viewsByUser.remove(user, view);
            if (viewsBySymbol.get(symbol).isEmpty()) {
                MDForSymbol remove = marketDataForSymbolMap.remove(symbol);
                if (null != remove) {
                    remove.unsubscribeForMD();
                }
            }
        }
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        final String data = msg.getData();
        final String[] args = data.split("\0");
        final String cmd = args[0];
        final ShredderView view = viewsBySocket.get(msg.getOutboundChannel());

        if (null != view) {
            if ("shredder-subscribe".equals(cmd)) {
                final String symbol = args[1];
                final int levels = Integer.parseInt(args[2]);
                final MDForSymbol mdForSymbol = marketDataForSymbolMap.get(symbol);
                view.subscribeToSymbol(symbol, levels, mdForSymbol, ordersBySymbol.get(symbol));
                viewsBySymbol.put(symbol, view);
            } else {
                view.onRawInboundData(data);
                view.refreshAndFlush();
            }
        }
    }

    @Subscribe
    public void on(final ServerTradingStatus serverTradingStatus) {
        if (!serverTradingStatus.isWorkingOrderConnected) {
            for (final WorkingOrdersForSymbol ordersForSymbol : ordersBySymbol.values()) {
                for (final Iterator<WorkingOrderUpdateFromServer> iter = ordersForSymbol.ordersByKey.values().iterator();
                     iter.hasNext(); ) {
                    final WorkingOrderUpdateFromServer working = iter.next();
                    if (working.fromServer.equals(serverTradingStatus.server)) {
                        iter.remove();
                    }
                }
                for (final Iterator<WorkingOrderUpdateFromServer> iter = ordersForSymbol.ordersByPrice.values().iterator();
                     iter.hasNext(); ) {
                    final WorkingOrderUpdateFromServer working = iter.next();
                    if (working.fromServer.equals(serverTradingStatus.server)) {
                        iter.remove();
                    }
                }
            }
        }
    }

    @Subscribe
    public void on(final WorkingOrderUpdateFromServer workingOrderUpdate) {
        ordersBySymbol.get(workingOrderUpdate.workingOrderUpdate.getSymbol()).onWorkingOrderUpdate(workingOrderUpdate);
    }

    private MDForSymbol subscribeToMarketDataForSymbol(final String symbol) {
        return new MDForSymbol(bookSubscriber, symbol);
    }

    public long flushAllShredders() {
        for (final ShredderView shredderView : viewsBySocket.values()) {
            shredderView.timedRefresh();
        }
        return BATCH_FLUSH_INTERVAL_MS;
    }

    public long sendAllHeartbeats() {
        for (final ShredderView shredderView : viewsBySocket.values()) {
            shredderView.sendHeartbeat();
        }
        return HEARTBEAT_INTERVAL_MS;
    }
}
