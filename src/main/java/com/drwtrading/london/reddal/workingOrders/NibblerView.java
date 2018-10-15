package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.photons.eeifoe.Cancel;
import com.drwtrading.london.photons.eeifoe.OrderSide;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.ShutdownOMSCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.StopAllStrategiesCmd;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.websockets.WebSocketOutboundData;
import eeif.execution.Side;
import eeif.execution.WorkingOrderState;
import eeif.execution.WorkingOrderUpdate;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class NibblerView {

    private final String server;
    private final Publisher<RemoteOrderCommandToServer> commands;
    private final Publisher<OrderEntryCommandToServer> managedOrderCommands;
    private final Map<String, SearchResult> searchResults;

    private final Map<String, WorkingOrderUpdateFromServer> workingOrders;
    private final Map<String, WorkingOrderUpdateFromServer> dirty;

    private final Map<String, UpdateFromServer> managedOrders;
    private final Map<String, UpdateFromServer> dirtyManaged;

    private final Set<Publisher<WebSocketOutboundData>> channels;
    private final IWorkingOrderView all;

    private boolean connected;

    NibblerView(final String server, final Publisher<RemoteOrderCommandToServer> commands,
            final Publisher<OrderEntryCommandToServer> managedOrderCommands, final Map<String, SearchResult> searchResults) {
        this.server = server;
        this.commands = commands;
        this.managedOrderCommands = managedOrderCommands;
        this.searchResults = searchResults;
        this.workingOrders = new HashMap<>();
        this.managedOrders = new HashMap<>();
        this.dirty = new HashMap<>();
        this.dirtyManaged = new HashMap<>();
        this.connected = false;
        this.channels = new HashSet<>();
        this.all = new WebSocketOutputDispatcher<>(IWorkingOrderView.class).wrap(msg -> channels.forEach(p -> p.publish(msg)));
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(final boolean connected) {
        if (connected == this.connected) {
            return;
        }
        this.connected = connected;
        if (connected) {
            final List<WorkingOrderUpdateFromServer> removed = new ArrayList<>(workingOrders.values());
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

    public void on(final WorkingOrderUpdateFromServer order) {
        if (order.workingOrderUpdate.getWorkingOrderState() == WorkingOrderState.DEAD) {
            workingOrders.remove(order.key());
        } else {
            workingOrders.put(order.key(), order);
        }
        dirty.put(order.key(), order);

    }

    public void on(final UpdateFromServer update) {
        if (update.update.isDead()) {
            managedOrders.remove(update.key);
        } else {
            managedOrders.put(update.key, update);
        }
        dirtyManaged.put(update.key, update);
    }

    public int getOrderCount() {
        return workingOrders.size() + managedOrders.size();
    }

    public void on(final ServerDisconnected serverDisconnected) {
        final List<UpdateFromServer> collect = managedOrders.values().stream().filter(
                updateFromServer -> serverDisconnected.server.equals(updateFromServer.server)).collect(Collectors.toList());
        for (final UpdateFromServer updateFromServer : collect) {
            managedOrders.remove(updateFromServer.key);
            dirtyManaged.remove(updateFromServer.key);
        }
    }

    public void sendUpdates() {
        sendUpdates(all, dirty, dirtyManaged);
        dirty.clear();
        dirtyManaged.clear();
    }

    public void register(final Publisher<WebSocketOutboundData> client, final IWorkingOrderView view) {
        channels.add(client);
        sendUpdates(view, workingOrders, managedOrders);
    }

    public void unregister(final Publisher<WebSocketOutboundData> client) {
        channels.remove(client);
    }

    private void sendUpdates(final IWorkingOrderView view, final Map<String, WorkingOrderUpdateFromServer> workingOrders,
            final Map<String, UpdateFromServer> managedOrders) {

        for (final WorkingOrderUpdateFromServer updated : workingOrders.values()) {
            publishWorkingOrderUpdate(view, updated);
        }
        for (final UpdateFromServer update : managedOrders.values()) {
            publishManagedOrderUpdate(view, update);
        }
    }

    private void publishManagedOrderUpdate(final IWorkingOrderView view, final UpdateFromServer order) {
        final SearchResult searchResult = searchResults.get(order.symbol);
        final String price;
        if (null != searchResult) {
            WorkingOrdersPresenter.DECIMAL_FORMAT.setMinimumFractionDigits(searchResult.decimalPlaces);
            WorkingOrdersPresenter.DECIMAL_FORMAT.setMaximumFractionDigits(searchResult.decimalPlaces);
            price = WorkingOrdersPresenter.DECIMAL_FORMAT.format(
                    order.update.getOrder().getPrice() / (double) Constants.NORMALISING_FACTOR);
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
            WorkingOrdersPresenter.DECIMAL_FORMAT.setMinimumFractionDigits(searchResult.decimalPlaces);
            WorkingOrdersPresenter.DECIMAL_FORMAT.setMaximumFractionDigits(searchResult.decimalPlaces);
            price = WorkingOrdersPresenter.DECIMAL_FORMAT.format(update.getPrice() / (double) Constants.NORMALISING_FACTOR);
        } else {
            price = update.getPrice() + " (raw)";
        }

        final String chainID = Integer.toString(order.workingOrderUpdate.getChainId());
        view.updateWorkingOrder(order.key(), chainID, update.getSymbol(), update.getSide().toString(), price, update.getFilledQuantity(),
                update.getTotalQuantity(), update.getWorkingOrderState().toString(), update.getWorkingOrderType().toString(),
                update.getTag(), order.fromServer, update.getWorkingOrderState() == WorkingOrderState.DEAD);
    }

    public void cancelAllNonGTC(final String user, final String reason, final boolean isAutomated) {
        workingOrders.values().stream().filter(WorkingOrdersPresenter.NON_GTC_FILTER).forEach(order -> cancel(user, order, isAutomated));
        managedOrders.values().forEach(this::cancel);
        stopAllStrategies(reason);
    }

    public void cancelAll(final String user, final String reason, final boolean isAUtomated) {
        workingOrders.values().forEach(order -> cancel(user, order, false));
        managedOrders.values().forEach(this::cancel);
        stopAllStrategies("Working orders - Cancel ALL exchange.");

    }

    private void stopAllStrategies(final String reason) {
        final IOrderCmd cmd = new StopAllStrategiesCmd(reason);
        final RemoteOrderCommandToServer command = new RemoteOrderCommandToServer(server, cmd);
        commands.publish(command);
    }

    public void shutdownOMS(final String reason) {
        final IOrderCmd remoteCommand = new ShutdownOMSCmd(reason);
        final RemoteOrderCommandToServer command = new RemoteOrderCommandToServer(server, remoteCommand);
        commands.publish(command);
        stopAllStrategies("Shutdown OMS - " + reason);
    }

    private void cancel(final String username, final WorkingOrderUpdateFromServer order, final boolean isAutomated) {

        final RemoteOrderCommandToServer cmd;
        if (isAutomated) {
            cmd = order.buildAutoCancel(username);
        } else {
            cmd = order.buildCancelCommand(username);
        }
        commands.publish(cmd);
    }

    private void cancel(final UpdateFromServer order) {
        managedOrderCommands.publish(
                new OrderEntryCommandToServer(order.server, new Cancel(order.update.getSystemOrderId(), order.update.getOrder())));
    }

    public void cancelOrder(final String user, final String key) {
        final UpdateFromServer updateFromServer = managedOrders.get(key);
        if (null != updateFromServer) {
            cancel(updateFromServer);
        } else {
            final WorkingOrderUpdateFromServer order = workingOrders.get(key);
            if (null != order) {
                cancel(user, order, false);
            }
        }
    }
}
