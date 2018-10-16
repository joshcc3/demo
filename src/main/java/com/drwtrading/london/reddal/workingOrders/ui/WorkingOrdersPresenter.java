package com.drwtrading.london.reddal.workingOrders.ui;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryFromServer;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.channels.Converter;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class WorkingOrdersPresenter {

    private static final long REPAINT_PERIOD_MILLIS = 500;
    private static final long HEART_BEAT_TIMEOUT_NANOS = 5 * DateTimeUtil.NANOS_IN_SECS;

    private final IClock clock;
    private final IResourceMonitor<ReddalComponents> monitor;

    private final UILogger webLog;
    private final FiberBuilder logFiber;

    private final Map<String, NibblerView> nibblers;

    private final Publisher<RemoteOrderCommandToServer> commands;
    private final Publisher<OrderEntryCommandToServer> managedOrderCommands;

    private final WebSocketViews<IWorkingOrderView> views;

    private final HashSet<String> dirtyServers;

    private int numViewers;
    private long lastViewerHeartbeatNanoSinceMidnight;

    public WorkingOrdersPresenter(final SelectIO selectIO, final IResourceMonitor<ReddalComponents> monitor, final UILogger webLog,
            final FiberBuilder logFiber, final Publisher<RemoteOrderCommandToServer> commands,
            final Publisher<OrderEntryCommandToServer> managedOrderCommands) {

        this.clock = selectIO;
        this.monitor = monitor;

        this.webLog = webLog;
        this.logFiber = logFiber;

        this.nibblers = new HashMap<>();

        this.commands = commands;
        this.managedOrderCommands = managedOrderCommands;

        this.views = WebSocketViews.create(IWorkingOrderView.class, this);
        this.numViewers = 0;

        this.dirtyServers = new HashSet<>();

        selectIO.addDelayedAction(3000, this::repaint);
    }

    public void addNibbler(final String nibbler) {
        this.nibblers.put(nibbler, new NibblerView(nibbler, commands, managedOrderCommands));
    }

    public static class WOConverter implements Converter<WorkingOrderUpdateFromServer, String> {

        @Override
        public String convert(final WorkingOrderUpdateFromServer msg) {
            return msg.key();
        }
    }

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final NibblerView nibblerView = nibblers.get(sourcedOrder.source);
        nibblerView.setWorkingOrder(sourcedOrder);

        dirtyServers.add(sourcedOrder.source);
    }

    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final NibblerView nibblerView = nibblers.get(sourcedOrder.source);
        nibblerView.deleteWorkingOrder(sourcedOrder);

        dirtyServers.add(sourcedOrder.source);
    }

    public void setNibblerConnectionEstablished(final String sourceNibbler, final boolean isConnected) {

        if (nibblers.containsKey(sourceNibbler)) {

            final NibblerView nibblerView = nibblers.get(sourceNibbler);
            nibblerView.setConnected(isConnected);
            dirtyServers.add(sourceNibbler);
            views.all().addNibbler(sourceNibbler, isConnected, nibblerView.getOrderCount());
        }
    }

    public void oeUpdate(final List<OrderEntryFromServer> oeUpdates) {

        for (final OrderEntryFromServer update : oeUpdates) {

            if (update instanceof UpdateFromServer) {
                setOEUpdate((UpdateFromServer) update);
            } else if (update instanceof ServerDisconnected) {
                setOEDisconnected((ServerDisconnected) update);
            }
        }
    }

    private void setOEUpdate(final UpdateFromServer update) {
        nibblers.get(update.server).on(update);
        dirtyServers.add(update.server);
    }

    private void setOEDisconnected(final ServerDisconnected serverDisconnected) {
        nibblers.get(serverDisconnected.server).on(serverDisconnected);

    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketConnected) {

            onConnected((WebSocketConnected) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            onDisconnected((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            onMessage((WebSocketInboundData) webMsg);
        }
    }

    private void onConnected(final WebSocketConnected connected) {

        final int remainingViewers = ++numViewers;
        logFiber.execute(() -> webLog.write("workingOrders", connected, true, remainingViewers));

        final IWorkingOrderView view = views.register(connected);
        for (final Map.Entry<String, NibblerView> nibbler : nibblers.entrySet()) {
            view.addNibbler(nibbler.getKey(), nibbler.getValue().isConnected(), nibbler.getValue().getOrderCount());
            view.refreshWorkingOrderCounts(nibbler.getKey(), nibbler.getValue().getOrderCount());
        }
    }

    private void onDisconnected(final WebSocketDisconnected disconnected) {

        final int remainingViewers = --numViewers;
        logFiber.execute(() -> webLog.write("workingOrders", disconnected, false, remainingViewers));

        views.unregister(disconnected);
        for (final NibblerView nibblerView : nibblers.values()) {
            nibblerView.unregister(disconnected.getOutboundChannel());
        }
    }

    public void onMessage(final WebSocketInboundData msg) {

        logFiber.execute(() -> webLog.write("workingOrders", msg));
        views.invoke(msg);
    }

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
        for (final NibblerView nibblerView : nibblers.values()) {
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
        for (final NibblerView nibblerView : nibblers.values()) {
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
        final NibblerView nibblerView = nibblers.get(server);
        nibblerView.cancelOrder(user, key);
    }

    private long repaint() {

        if (0 < numViewers) {
            for (final String dirtyServer : dirtyServers) {
                final NibblerView nibblerView = nibblers.get(dirtyServer);
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

        return REPAINT_PERIOD_MILLIS;
    }
}
