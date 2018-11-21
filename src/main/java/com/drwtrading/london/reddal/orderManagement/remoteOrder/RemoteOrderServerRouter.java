package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.reddal.ladders.LadderClickTradingIssue;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.SubmitOrderCmd;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrdersByUIKey;
import org.jetlang.channels.Channel;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RemoteOrderServerRouter {

    private final Channel<LadderClickTradingIssue> cancelRejectPublisher;

    private final Map<String, Integer> nibblerPriorities;

    private final Map<String, NibblerTransportOrderEntry> nibblers;
    private final Map<String, NibblerSymbolHandler> symbolVenues;

    private final Map<String, SourcedWorkingOrdersByUIKey> symbolWorkingOrder;

    public RemoteOrderServerRouter(final Channel<LadderClickTradingIssue> cancelRejectPublisher, final String[] nibblerPriorities) {

        this.cancelRejectPublisher = cancelRejectPublisher;

        this.nibblerPriorities = new HashMap<>();

        int i = 0;
        for (final String nibbler : nibblerPriorities) {
            this.nibblerPriorities.put(nibbler, ++i);
        }

        this.nibblers = new HashMap<>();
        this.symbolVenues = new HashMap<>();

        this.symbolWorkingOrder = new HashMap<>();
    }

    public void addNibbler(final String nibblerName, final NibblerTransportOrderEntry entry) {

        nibblers.put(nibblerName, entry);
    }

    public void setInstrumentTradable(final String symbol, final Set<OrderType> supportedOrderTypes, final String nibblerName) {

        final int priority = nibblerPriorities.get(nibblerName);
        final NibblerTransportOrderEntry orderEntry = nibblers.get(nibblerName);

        final NibblerSymbolHandler prevVenue = symbolVenues.get(symbol);

        if (null == prevVenue || priority < prevVenue.priority) {

            final NibblerSymbolHandler nibblerSymbolHandler =
                    new NibblerSymbolHandler(priority, nibblerName, supportedOrderTypes, orderEntry);
            symbolVenues.put(symbol, nibblerSymbolHandler);

            if (null != prevVenue) {
                cancelAllOldSymbolOrders(symbol, nibblerName);
            }
        }
    }

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {
        final SourcedWorkingOrdersByUIKey workingOrders = symbolWorkingOrder.get(sourcedOrder.order.getSymbol());
        if (null == workingOrders) {
            final SourcedWorkingOrdersByUIKey newWorkingOrders = new SourcedWorkingOrdersByUIKey();
            symbolWorkingOrder.put(sourcedOrder.order.getSymbol(), newWorkingOrders);
            newWorkingOrders.setWorkingOrder(sourcedOrder);
        } else {
            workingOrders.setWorkingOrder(sourcedOrder);
        }
    }

    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder) {
        final SourcedWorkingOrdersByUIKey workingOrders = symbolWorkingOrder.get(sourcedOrder.order.getSymbol());
        workingOrders.removeWorkingOrder(sourcedOrder);
    }

    public void setNibblerDisconnected(final String disconnectedNibbler) {

        for (final SourcedWorkingOrdersByUIKey workingOrders : symbolWorkingOrder.values()) {

            workingOrders.clearNibblerOrders(disconnectedNibbler);
        }
    }

    private void cancelAllOldSymbolOrders(final String symbol, final String currentNibblerName) {
        final SourcedWorkingOrdersByUIKey workingOrders = symbolWorkingOrder.get(symbol);
        if (null != workingOrders) {
            for (final SourcedWorkingOrder sourcedOrder : workingOrders.getWorkingOrders()) {
                if (!currentNibblerName.equals(sourcedOrder.source)) {
                    final NibblerTransportOrderEntry nibbler = nibblers.get(sourcedOrder.source);
                    nibbler.cancelOrder(cancelRejectPublisher, "AUTOMATED", true, sourcedOrder.order.getChainID(),
                            sourcedOrder.order.getSymbol());
                }
            }
        }
    }

    public void submitOrder(final SubmitOrderCmd submit) {

        final String symbol = submit.getSymbol();
        final NibblerSymbolHandler nibbler = symbolVenues.get(symbol);

        if (null == nibbler) {
            submit.rejectMsg("No nibbler to send submit.");
        } else if (!nibbler.supportedOrderTypes.contains(submit.getOrderType())) {
            submit.rejectMsg("Order type [" + submit.getOrderType() + "] not supported by Nibbler.");
        } else {
            submit.execute(nibbler.orderEntry);
        }
    }

    public void routeCmd(final String nibblerName, final IOrderCmd cmd) {

        final NibblerTransportOrderEntry nibbler = nibblers.get(nibblerName);
        if (null == nibbler) {
            cmd.rejectMsg("Unknown nibbler [" + nibblerName + "] for [" + cmd.getClass().getSimpleName() + "].");
        } else {
            cmd.execute(nibbler);
        }
    }

    public void broadcastCmd(final IOrderCmd cmd) {

        for (final NibblerTransportOrderEntry nibbler : nibblers.values()) {
            cmd.execute(nibbler);
        }
    }
}
