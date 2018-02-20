package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.jetlang.autosubscribe.BatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.KeyedBatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.ShutdownOMSCmd;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.MapMaker;
import org.jetlang.channels.Converter;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Scheduler;

import java.text.DecimalFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class WorkingOrdersPresenter {

    private static final long HEART_BEAT_TIMEOUT_NANOS = 5 * DateTimeUtil.NANOS_IN_SECS;
    public static final Predicate<WorkingOrderUpdateFromServer> NON_GTC_FILTER = order -> !order.isLikelyGTC();
    public static final DecimalFormat DECIMAL_FORMAT = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS);

    private final IClock clock;
    private final IResourceMonitor<ReddalComponents> monitor;
    private final UILogger webLog;

    private final Publisher<RemoteOrderCommandToServer> commands;

    private final Map<String, NibblerView> nibblers;
    private final Map<String, SearchResult> searchResults;

    private final WebSocketViews<IWorkingOrderView> views;

    private final HashSet<String> dirtyServers;

    private int numViewers;
    private long lastViewerHeartbeatNanoSinceMidnight;


    public WorkingOrdersPresenter(final IClock clock, final IResourceMonitor<ReddalComponents> monitor, final UILogger webLog,
                                  final Scheduler scheduler, final Publisher<RemoteOrderCommandToServer> commands, final Collection<String> nibblers,
                                  final Publisher<OrderEntryCommandToServer> managedOrderCommands) {

        this.clock = clock;
        this.monitor = monitor;
        this.webLog = webLog;

        this.commands = commands;

        this.searchResults = new HashMap<>();
        this.nibblers = new MapMaker().makeComputingMap(server -> new NibblerView(server, commands, managedOrderCommands, searchResults));

        for (final String nibbler : nibblers) {
            this.nibblers.put(nibbler, new NibblerView(nibbler, commands, managedOrderCommands, searchResults));
        }

        this.views = WebSocketViews.create(IWorkingOrderView.class, this);
        this.numViewers = 0;

        scheduler.scheduleWithFixedDelay(this::repaint, 3000, 500, TimeUnit.MILLISECONDS);
        dirtyServers = new HashSet<>();
    }

    public void addSearchResult(final SearchResult searchResult) {
        searchResults.put(searchResult.symbol, searchResult);
    }

    public static class WOConverter implements Converter<WorkingOrderUpdateFromServer, String> {

        @Override
        public String convert(final WorkingOrderUpdateFromServer msg) {
            return msg.key();
        }
    }

    @KeyedBatchSubscriber(converter = WOConverter.class, flushInterval = 500, timeUnit = TimeUnit.MILLISECONDS)
    @Subscribe
    public void onWorkingOrder(final Map<String, WorkingOrderUpdateFromServer> orders) {
        for (final WorkingOrderUpdateFromServer order : orders.values()) {
            nibblers.get(order.fromServer).on(order);
            dirtyServers.add(order.fromServer);
        }
    }

    @BatchSubscriber
    @Subscribe
    public void on(final List<UpdateFromServer> updates) {
        for (final UpdateFromServer update : updates) {
            nibblers.get(update.server).on(update);
            dirtyServers.add(update.server);
        }
    }

    @Subscribe
    public void on(final ServerDisconnected serverDisconnected) {
        nibblers.get(serverDisconnected.server).on(serverDisconnected);

    }

    public void nibblerConnectionEstablished(final WorkingOrderConnectionEstablished connectionEstablished) {
        NibblerView nibblerView = nibblers.get(connectionEstablished.server);
        nibblerView.setConnected(connectionEstablished.established);
        dirtyServers.add(connectionEstablished.server);
        views.all().addNibbler(connectionEstablished.server, connectionEstablished.established, nibblerView.getOrderCount());
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        webLog.write("workingOrders", connected, true, ++numViewers);
        final IWorkingOrderView view = views.register(connected);
        for (final Map.Entry<String, NibblerView> nibbler : nibblers.entrySet()) {
            view.addNibbler(nibbler.getKey(), nibbler.getValue().isConnected(), nibbler.getValue().getOrderCount());
            view.refreshWorkingOrderCounts(nibbler.getKey(), nibbler.getValue().getOrderCount());
        }
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {
        webLog.write("workingOrders", disconnected, false, --numViewers);
        views.unregister(disconnected);
        for (NibblerView nibblerView : nibblers.values()) {
            nibblerView.unregister(disconnected.getOutboundChannel());
        }
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        webLog.write("workingOrders", msg);
        views.invoke(msg);
    }

    // --------------


    @FromWebSocketView
    public void watchServer(final String server, final WebSocketInboundData client) {
        nibblers.get(server).register(client.getOutboundChannel(), views.get(client.getOutboundChannel()));
    }

    @FromWebSocketView
    public void unwatchServer(final String server, final WebSocketInboundData client) {
        nibblers.get(server).unregister(client.getOutboundChannel());
    }

    @FromWebSocketView
    public void heartbeat(final WebSocketInboundData data) {
        this.lastViewerHeartbeatNanoSinceMidnight = clock.getReferenceNanoSinceMidnightUTC();
    }

    @FromWebSocketView
    public void shutdownAll(final WebSocketInboundData data) {
        for (final NibblerView nibbler : nibblers.values()) {
            nibbler.shutdownOMS("Working orders - Shutdown ALL exchanges.");
        }
    }

    @FromWebSocketView
    public void shutdownExchange(final WebSocketInboundData data, final String nibbler) {
        nibblers.get(nibbler).shutdownOMS("Working orders - Shutdown exchange.");
    }

    @FromWebSocketView
    public void cancelAllNonGTC(final WebSocketInboundData data) {
        final String user = data.getClient().getUserName();
        cancelAllNonGTC(user, "Working orders - Cancel non-gtc.", false);
    }

    private void cancelAllNonGTC(final String user, final String reason, final boolean isAutomated) {
        for (NibblerView nibblerView : nibblers.values()) {
            nibblerView.cancelAllNonGTC(user, reason, isAutomated);

        }
    }

    @FromWebSocketView
    public void cancelExchangeNonGTC(final WebSocketInboundData data, final String nibbler) {
        final String user = data.getClient().getUserName();
        nibblers.get(nibbler).cancelAllNonGTC(user, "Working orders - Cancel exchange non-gtc.", false);
    }

    @FromWebSocketView
    public void cancelAll(final WebSocketInboundData data) {
        final String user = data.getClient().getUserName();
        for (NibblerView nibblerView : nibblers.values()) {
            nibblerView.cancelAll(user, "Working orders - Cancel ALL exchange.", false);
        }
    }

    @FromWebSocketView
    public void cancelExchange(final WebSocketInboundData data, final String nibbler) {
        final String user = data.getClient().getUserName();
        nibblers.get(nibbler).cancelAll(user, "Working orders - Cancel ALL.", false);
    }


    @FromWebSocketView
    public void cancelOrder(final String server, final String key, final WebSocketInboundData data) {
        final String user = data.getClient().getUserName();
        NibblerView nibblerView = nibblers.get(server);
        nibblerView.cancelOrder(user, key);
    }

    // -----------------

    private void repaint() {

        if (0 < numViewers) {
            for (String dirtyServer : dirtyServers) {
                NibblerView nibblerView = nibblers.get(dirtyServer);
                nibblerView.sendUpdates();
                views.all().refreshWorkingOrderCounts(dirtyServer, nibblerView.getOrderCount());
            }
        }

        dirtyServers.clear();

        final boolean userHeartbeatLate =
                HEART_BEAT_TIMEOUT_NANOS < (clock.getReferenceNanoSinceMidnightUTC() - lastViewerHeartbeatNanoSinceMidnight);
        if (userHeartbeatLate) {
            monitor.logError(ReddalComponents.SAFETY_WORKING_ORDER_VIEWER, "No users viewing working order screen.");
            cancelAllNonGTC("AUTOMATED", "Working orders - no users viewing working orders screen.", true);
        } else {
            monitor.setOK(ReddalComponents.SAFETY_WORKING_ORDER_VIEWER);
        }
    }

}
