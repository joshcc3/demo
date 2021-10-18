package com.drwtrading.london.reddal.workingOrders.ui;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.oe.OrderEntryCommandToServer;
import com.drwtrading.london.reddal.orderManagement.oe.ServerDisconnected;
import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.ShutdownOMSCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.StopAllStrategiesCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.TraderLoginCmd;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.websocket.WebSocketOutputDispatcher;
import com.drwtrading.websockets.WebSocketOutboundData;
import drw.eeif.eeifoe.Cancel;
import drw.eeif.eeifoe.OrderSide;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

class NibblerView {

    private final String server;
    private final Publisher<IOrderCmd> commands;
    private final Publisher<LadderClickTradingIssue> cmdRejectPublisher;
    private final Publisher<OrderEntryCommandToServer> managedOrderCommands;

    private final Map<String, SourcedWorkingOrder> workingOrders;
    private final Set<String> dirty;

    private final Map<String, UpdateFromServer> managedOrders;
    private final Map<String, UpdateFromServer> dirtyManaged;

    private final Set<Publisher<WebSocketOutboundData>> channels;
    private final IWorkingOrderView all;

    private final DecimalFormat priceDF;
    private boolean isConnected;

    NibblerView(final String server, final Publisher<IOrderCmd> commands, final Publisher<LadderClickTradingIssue> cmdRejectPublisher,
            final Publisher<OrderEntryCommandToServer> managedOrderCommands) {

        this.server = server;
        this.commands = commands;
        this.cmdRejectPublisher = cmdRejectPublisher;
        this.managedOrderCommands = managedOrderCommands;
        this.workingOrders = new HashMap<>();
        this.managedOrders = new HashMap<>();
        this.dirty = new HashSet<>();
        this.dirtyManaged = new HashMap<>();
        this.isConnected = false;

        this.channels = new HashSet<>();
        this.all = new WebSocketOutputDispatcher<>(IWorkingOrderView.class).wrap(msg -> channels.forEach(p -> p.publish(msg)));

        this.priceDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 0, 10);
    }

    public boolean isConnected() {
        return isConnected;
    }

    public void setConnected(final boolean isConnected) {

        if (isConnected != this.isConnected) {

            this.isConnected = isConnected;
            if (isConnected) {
                dirty.addAll(workingOrders.keySet());
                workingOrders.clear();
            }
        }
    }

    void setWorkingOrder(final SourcedWorkingOrder order) {

        workingOrders.put(order.uiKey, order);
        dirty.add(order.uiKey);
    }

    void deleteWorkingOrder(final SourcedWorkingOrder order) {

        workingOrders.remove(order.uiKey);
        dirty.add(order.uiKey);
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
        sendUpdates(view, workingOrders.keySet(), managedOrders);
    }

    public void unregister(final Publisher<WebSocketOutboundData> client) {
        channels.remove(client);
    }

    private void sendUpdates(final IWorkingOrderView view, final Set<String> dirtyWorkingOrdersIDs,
            final Map<String, UpdateFromServer> managedOrders) {

        for (final String updatedID : dirtyWorkingOrdersIDs) {

            final SourcedWorkingOrder sourcedOrder = workingOrders.get(updatedID);
            if (null == sourcedOrder) {
                view.deleteWorkingOrder(updatedID);
            } else {
                publishWorkingOrderUpdate(view, sourcedOrder);
            }
        }
        for (final UpdateFromServer update : managedOrders.values()) {
            publishManagedOrderUpdate(view, update);
        }
    }

    private void publishManagedOrderUpdate(final IWorkingOrderView view, final UpdateFromServer order) {

        final String price = priceDF.format(order.update.getOrder().getPrice() / (double) Constants.NORMALISING_FACTOR);

        final String chainID = Integer.toString(order.update.getSystemOrderId());
        final OrderSide side = order.update.getOrder().getSide();

        view.setWorkingOrder(order.key, chainID, order.symbol, side == OrderSide.BUY ? BookSide.BID.name() : BookSide.ASK.name(), price,
                order.update.getFilledQty(), order.update.getFilledQty() + order.update.getRemainingQty(), "MANAGED",
                order.update.getOrder().getUser(), order.server);
    }

    private void publishWorkingOrderUpdate(final IWorkingOrderView view, final SourcedWorkingOrder sourcedOrder) {

        final WorkingOrder order = sourcedOrder.order;
        final String price = priceDF.format(order.getPrice() / (double) Constants.NORMALISING_FACTOR);

        final String chainID = Integer.toString(order.getChainID());
        view.setWorkingOrder(sourcedOrder.uiKey, chainID, order.getSymbol(), order.getSide().toString(), price, order.getFilledQty(),
                order.getOrderQty(), order.getAlgoType().name() + '-' + order.getOrderType().name(), order.getTag().name(),
                sourcedOrder.source);
    }

    public void cancelAllNonGTC(final User user, final String reason, final boolean isAutomated) {

        stopAllStrategies(reason);

        for (final SourcedWorkingOrder sourcedOrder : workingOrders.values()) {
            if (OrderType.GTC != sourcedOrder.order.getOrderType()) {
                cancel(user, sourcedOrder, isAutomated);
            }
        }
        managedOrders.values().forEach(this::cancel);
    }

    public void cancelAll(final User user, final String reason, final boolean isAutomated) {

        workingOrders.values().forEach(order -> cancel(user, order, isAutomated));
        managedOrders.values().forEach(this::cancel);
        stopAllStrategies(reason);
    }

    private void stopAllStrategies(final String reason) {
        final IOrderCmd cmd = new StopAllStrategiesCmd(server, reason);
        commands.publish(cmd);
    }

    public void shutdownOMS(final String reason) {
        final IOrderCmd cmd = new ShutdownOMSCmd(server, reason);
        commands.publish(cmd);
        stopAllStrategies("Shutdown OMS - " + reason);
    }

    private void cancel(final User user, final SourcedWorkingOrder order, final boolean isAutomated) {

        final IOrderCmd cmd = order.buildCancel(cmdRejectPublisher, user, isAutomated);
        commands.publish(cmd);
    }

    private void cancel(final UpdateFromServer order) {
        managedOrderCommands.publish(
                new OrderEntryCommandToServer(order.server, new Cancel(order.update.getSystemOrderId(), order.update.getOrder())));
    }

    public void cancelOrder(final User user, final String key) {

        final UpdateFromServer updateFromServer = managedOrders.get(key);
        if (null != updateFromServer) {
            cancel(updateFromServer);
        } else {
            final SourcedWorkingOrder order = workingOrders.get(key);
            if (null != order) {
                cancel(user, order, false);
            }
        }
    }

    void traderLogin(final Set<User> users) {

        final TraderLoginCmd cmd = new TraderLoginCmd(server, users);
        commands.publish(cmd);
    }
}
