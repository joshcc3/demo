package com.drwtrading.london.reddal.orderManagement.remoteOrder;

import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.SubmitOrderCmd;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrdersByUIKey;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class RemoteOrderServerRouter {

    private final Map<String, Integer> nibblerPriorities;
    private final Map<String, NibblerTransportOrderEntry> nibblers;
    private final Map<String, NibblerTransportOrderEntry> broadcastNibblers;
    private final EnumMap<OrderType, Map<String, NibblerSymbolHandler>> symbolVenuesByOrderType;

    private final Map<String, SourcedWorkingOrdersByUIKey> symbolWorkingOrder;

    public RemoteOrderServerRouter(final String[] nibblerPriorities) {

        this.nibblerPriorities = new HashMap<>();

        int i = 0;
        for (final String nibbler : nibblerPriorities) {
            this.nibblerPriorities.put(nibbler, ++i);
        }

        this.nibblers = new HashMap<>();
        this.broadcastNibblers = new HashMap<>();
        this.symbolVenuesByOrderType = new EnumMap<>(OrderType.class);
        this.symbolWorkingOrder = new HashMap<>();
    }

    public void addNibbler(final String nibblerName, final NibblerTransportOrderEntry entry) {

        nibblers.put(nibblerName, entry);
        broadcastNibblers.put(nibblerName, entry);
    }

    public void addNonTradableNibbler(final String nibblerName, final NibblerTransportOrderEntry entry) {

        broadcastNibblers.put(nibblerName, entry);
    }

    public void setInstrumentTradable(final String symbol, final Set<OrderType> supportedOrderTypes, final String nibblerName) {

        for (final OrderType supportedOrderType : supportedOrderTypes) {
            final Map<String, NibblerSymbolHandler> symbolVenues = getSymbolVenues(supportedOrderType);

            final int priority = nibblerPriorities.get(nibblerName);
            final NibblerTransportOrderEntry orderEntry = nibblers.get(nibblerName);
            final NibblerSymbolHandler prevVenue = symbolVenues.get(symbol);

            if (null == prevVenue || priority < prevVenue.priority) {
                final NibblerSymbolHandler nibblerSymbolHandler =
                        new NibblerSymbolHandler(priority, nibblerName, supportedOrderTypes, orderEntry);
                symbolVenues.put(symbol, nibblerSymbolHandler);
            }
        }

    }

    private Map<String, NibblerSymbolHandler> getSymbolVenues(final OrderType orderType) {
        return symbolVenuesByOrderType.computeIfAbsent(orderType, s -> new HashMap<>());
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

    public void submitOrder(final SubmitOrderCmd submit) {

        final String symbol = submit.getSymbol();
        final NibblerSymbolHandler nibbler = getSymbolVenues(submit.getOrderType()).get(symbol);

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

        for (final NibblerTransportOrderEntry nibbler : broadcastNibblers.values()) {
            cmd.execute(nibbler);
        }
    }
}
