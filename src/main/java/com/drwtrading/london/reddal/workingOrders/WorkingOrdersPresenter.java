package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.jetlang.autosubscribe.BatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.KeyedBatchSubscriber;
import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.photons.eeifoe.Cancel;
import com.drwtrading.london.photons.eeifoe.OrderSide;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.ShutdownOMSCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.StopAllStrategiesCmd;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import eeif.execution.Side;
import eeif.execution.WorkingOrderState;
import eeif.execution.WorkingOrderUpdate;
import org.jetlang.channels.Converter;
import org.jetlang.channels.Publisher;
import org.jetlang.core.Scheduler;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class WorkingOrdersPresenter {

    private static final long HEART_BEAT_TIMEOUT_NANOS = 5 * DateTimeUtil.NANOS_IN_SECS;
    public static final Predicate<WorkingOrderUpdateFromServer> NON_GTC_FILTER = order -> !order.isLikelyGTC();

    private final IClock clock;
    private final IResourceMonitor<ReddalComponents> monitor;
    private final UILogger webLog;

    private final Publisher<RemoteOrderCommandToServer> commands;
    private final Publisher<OrderEntryCommandToServer> managedOrderCommands;

    private final Map<String, Boolean> nibblersStatus;

    private final WebSocketViews<IWorkingOrderView> views;
    private final Map<String, WorkingOrderUpdateFromServer> workingOrders;
    private final Map<String, WorkingOrderUpdateFromServer> dirty;
    private final Map<String, SearchResult> searchResults;

    private final Map<String, UpdateFromServer> managedOrders;
    private final Map<String, UpdateFromServer> dirtyManaged;

    private final DecimalFormat df;

    private int numViewers;
    private long lastViewerHeartbeatNanoSinceMidnight;

    public WorkingOrdersPresenter(final IClock clock, final IResourceMonitor<ReddalComponents> monitor, final UILogger webLog,
            final Scheduler scheduler, final Publisher<RemoteOrderCommandToServer> commands, final Collection<String> nibblers,
            final Publisher<OrderEntryCommandToServer> managedOrderCommands) {

        this.clock = clock;
        this.monitor = monitor;
        this.webLog = webLog;

        this.commands = commands;
        this.managedOrderCommands = managedOrderCommands;

        this.nibblersStatus = new HashMap<>();
        for (final String nibbler : nibblers) {
            nibblersStatus.put(nibbler, false);
        }

        this.views = WebSocketViews.create(IWorkingOrderView.class, this);
        this.workingOrders = new HashMap<>();
        this.managedOrders = new HashMap<>();
        this.dirty = new HashMap<>();
        this.dirtyManaged = new HashMap<>();
        this.searchResults = new HashMap<>();

        this.df = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS);

        this.numViewers = 0;

        scheduler.scheduleWithFixedDelay(this::repaint, 3000, 250, TimeUnit.MILLISECONDS);
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
            if (order.workingOrderUpdate.getWorkingOrderState() == WorkingOrderState.DEAD) {
                workingOrders.remove(order.key());
            } else {
                workingOrders.put(order.key(), order);
            }
            dirty.put(order.key(), order);
        }
    }

    @BatchSubscriber
    @Subscribe
    public void on(final List<UpdateFromServer> updates) {
        for (final UpdateFromServer update : updates) {
            if (update.update.isDead()) {
                managedOrders.remove(update.key);
            } else {
                managedOrders.put(update.key, update);
            }
            dirtyManaged.put(update.key, update);
        }
    }

    @Subscribe
    public void on(final ServerDisconnected serverDisconnected) {
        final List<UpdateFromServer> collect = managedOrders.values().stream().filter(
                updateFromServer -> serverDisconnected.server.equals(updateFromServer.server)).collect(Collectors.toList());
        for (final UpdateFromServer updateFromServer : collect) {
            managedOrders.remove(updateFromServer.key);
            dirtyManaged.remove(updateFromServer.key);
        }
    }

    public void nibblerConnectionEstablished(final WorkingOrderConnectionEstablished connectionEstablished) {

        nibblersStatus.put(connectionEstablished.server, connectionEstablished.established);
        views.all().addNibbler(connectionEstablished.server, connectionEstablished.established);

        if (connectionEstablished.established) {
            final List<WorkingOrderUpdateFromServer> removed = new LinkedList<>();
            for (final WorkingOrderUpdateFromServer update : workingOrders.values()) {
                if (connectionEstablished.server.equals(update.fromServer)) {
                    removed.add(update);
                }
            }

            for (final WorkingOrderUpdateFromServer update : removed) {
                workingOrders.remove(update.key());

                final WorkingOrderUpdate prev = update.workingOrderUpdate;
                final WorkingOrderUpdate delete =
                        new WorkingOrderUpdate(prev.getServerName(), prev.getSymbol(), prev.getTag(), prev.getChainId(), prev.getPrice(),
                                prev.getTotalQuantity(), prev.getFilledQuantity(), prev.getSide(), WorkingOrderState.DEAD,
                                prev.getWorkingOrderType());
                final WorkingOrderUpdateFromServer deleteUpdate = new WorkingOrderUpdateFromServer(update.fromServer, delete);
                dirty.put(deleteUpdate.key(), deleteUpdate);
            }
        }
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {

        webLog.write("workingOrders", connected, true, ++numViewers);
        final IWorkingOrderView view = views.register(connected);

        for (final Map.Entry<String, Boolean> nibbler : nibblersStatus.entrySet()) {
            view.addNibbler(nibbler.getKey(), nibbler.getValue());
        }
        for (final WorkingOrderUpdateFromServer update : workingOrders.values()) {
            publishWorkingOrderUpdate(view, update);
        }
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {

        webLog.write("workingOrders", disconnected, false, --numViewers);
        views.unregister(disconnected);
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {
        webLog.write("workingOrders", msg);
        views.invoke(msg);
    }

    // --------------

    @FromWebSocketView
    public void heartbeat(final WebSocketInboundData data) {

        this.lastViewerHeartbeatNanoSinceMidnight = clock.getReferenceNanoSinceMidnightUTC();
    }

    @FromWebSocketView
    public void shutdownAll(final WebSocketInboundData data) {

        for (final String nibbler : nibblersStatus.keySet()) {
            shutdownOMS(nibbler, "Working orders - Shutdown ALL exchanges.");
        }
    }

    @FromWebSocketView
    public void shutdownExchange(final WebSocketInboundData data, final String nibbler) {

        shutdownOMS(nibbler, "Working orders - Shutdown exchange.");
    }

    private void shutdownOMS(final String nibbler, final String reason) {

        final IOrderCmd remoteCommand = new ShutdownOMSCmd(reason);
        final RemoteOrderCommandToServer command = new RemoteOrderCommandToServer(nibbler, remoteCommand);
        commands.publish(command);
    }

    @FromWebSocketView
    public void cancelAllNonGTC(final WebSocketInboundData data) {

        final String user = data.getClient().getUserName();
        cancelAllNoneGTC(user, "Working orders - Cancel non-gtc.");
    }

    private void cancelAllNoneGTC(final String user, final String reason) {
        workingOrders.values().stream().filter(NON_GTC_FILTER).forEach(order -> cancel(user, order));
        for (final String nibbler : nibblersStatus.keySet()) {
            stopAllStrategies(nibbler, reason);
        }
        managedOrders.values().forEach(this::cancel);
    }

    @FromWebSocketView
    public void cancelExchangeNonGTC(final WebSocketInboundData data, final String nibbler) {
        final String user = data.getClient().getUserName();
        workingOrders.values().stream().filter(order -> nibbler.equals(order.fromServer)).filter(NON_GTC_FILTER).forEach(
                order -> cancel(user, order));
        managedOrders.values().stream().filter(order -> nibbler.equals(order.server)).forEach(this::cancel);
        stopAllStrategies(nibbler, "Working orders - Cancel exchange non-gtc.");
    }

    @FromWebSocketView
    public void cancelAll(final WebSocketInboundData data) {
        final String user = data.getClient().getUserName();
        workingOrders.values().forEach(order -> cancel(user, order));
        managedOrders.values().forEach(this::cancel);
        for (final String nibbler : nibblersStatus.keySet()) {
            stopAllStrategies(nibbler, "Working orders - Cancel ALL exchange.");
        }
    }

    @FromWebSocketView
    public void cancelExchange(final WebSocketInboundData data, final String nibbler) {
        final String user = data.getClient().getUserName();
        workingOrders.values().stream().filter(order -> nibbler.equals(order.fromServer)).forEach(order -> cancel(user, order));
        managedOrders.values().stream().filter(order -> nibbler.equals(order.server)).forEach(this::cancel);
        stopAllStrategies(nibbler, "Working orders - Cancel ALL.");
    }

    private void stopAllStrategies(final String nibbler, final String reason) {

        final IOrderCmd cmd = new StopAllStrategiesCmd(reason);
        final RemoteOrderCommandToServer command = new RemoteOrderCommandToServer(nibbler, cmd);
        commands.publish(command);
    }

    @FromWebSocketView
    public void cancelOrder(final String key, final WebSocketInboundData data) {

        final String user = data.getClient().getUserName();

        final UpdateFromServer updateFromServer = managedOrders.get(key);
        if (null != updateFromServer) {
            cancel(updateFromServer);
        } else {
            final WorkingOrderUpdateFromServer order = workingOrders.get(key);
            if (null != order) {
                cancel(user, order);
            }
        }
    }

    // -----------------

    private void cancel(final String username, final WorkingOrderUpdateFromServer order) {

        final RemoteOrderCommandToServer cmd = order.buildCancelCommand(username);
        commands.publish(cmd);
    }

    private void cancel(final UpdateFromServer order) {
        managedOrderCommands.publish(
                new OrderEntryCommandToServer(order.server, new Cancel(order.update.getSystemOrderId(), order.update.getOrder())));
    }

    private void repaint() {

        if (0 < numViewers) {
            for (final WorkingOrderUpdateFromServer workingOrderUpdate : dirty.values()) {
                publishWorkingOrderUpdate(views.all(), workingOrderUpdate);
            }
            for (final UpdateFromServer updateFromServer : dirtyManaged.values()) {
                publishManagedOrderUpdate(views.all(), updateFromServer);
            }
        }

        final boolean userHeartbeatLate =
                HEART_BEAT_TIMEOUT_NANOS < (clock.getReferenceNanoSinceMidnightUTC() - lastViewerHeartbeatNanoSinceMidnight);
        if (userHeartbeatLate) {
            monitor.logError(ReddalComponents.SAFETY_WORKING_ORDER_VIEWER, "No users viewing working order screen.");
            cancelAllNoneGTC("AUTOMATED", "Working orders - no users viewing working orders screen.");
        } else {
            monitor.setOK(ReddalComponents.SAFETY_WORKING_ORDER_VIEWER);
        }
        dirty.clear();
        dirtyManaged.clear();
    }

    private void publishManagedOrderUpdate(final IWorkingOrderView view, final UpdateFromServer order) {
        final SearchResult searchResult = searchResults.get(order.symbol);
        final String price;
        if (null != searchResult) {
            df.setMinimumFractionDigits(searchResult.decimalPlaces);
            df.setMaximumFractionDigits(searchResult.decimalPlaces);
            price = df.format(order.update.getOrder().getPrice() / (double) Constants.NORMALISING_FACTOR);
        } else {
            price = order.update.getOrder().getPrice() + " (raw)";
        }

        final String chainID = Integer.toString(order.update.getSystemOrderId());
        final OrderSide side = order.update.getOrder().getSide();

        view.updateWorkingOrder(order.key, chainID, order.symbol, side == OrderSide.BUY ? Side.BID.toString() : Side.OFFER.toString(),
                price, order.update.getFilledQty(), order.update.getFilledQty() + order.update.getRemainingQty(), order.update.getState(),
                "MANAGED", order.update.getOrder().getUser(), order.server, order.update.isDead());
    }

    private void publishWorkingOrderUpdate(final IWorkingOrderView view, final WorkingOrderUpdateFromServer order) {
        final WorkingOrderUpdate update = order.workingOrderUpdate;
        final SearchResult searchResult = searchResults.get(update.getSymbol());
        final String price;
        if (null != searchResult) {
            df.setMinimumFractionDigits(searchResult.decimalPlaces);
            df.setMaximumFractionDigits(searchResult.decimalPlaces);
            price = df.format(update.getPrice() / (double) Constants.NORMALISING_FACTOR);
        } else {
            price = update.getPrice() + " (raw)";
        }

        final String chainID = Integer.toString(order.workingOrderUpdate.getChainId());
        view.updateWorkingOrder(order.key(), chainID, update.getSymbol(), update.getSide().toString(), price, update.getFilledQuantity(),
                update.getTotalQuantity(), update.getWorkingOrderState().toString(), update.getWorkingOrderType().toString(),
                update.getTag(), order.fromServer, update.getWorkingOrderState() == WorkingOrderState.DEAD);
    }
}
