package com.drwtrading.london.reddal.workingOrders.gtc;

import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrdersByUIKey;

import java.util.HashMap;
import java.util.Map;

public class GTCWorkingOrderMaintainer implements IWorkingOrdersCallback {

    private final OPXLGTCWorkingOrdersPresenter opxlWriter;

    private final Map<String, SourcedWorkingOrdersByUIKey> workingOrders;

    public GTCWorkingOrderMaintainer(final OPXLGTCWorkingOrdersPresenter opxlWriter) {

        this.opxlWriter = opxlWriter;

        this.workingOrders = new HashMap<>();
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        if (OrderType.GTC == workingOrder.order.getOrderType()) {

            final String symbol = workingOrder.order.getSymbol();
            final SourcedWorkingOrdersByUIKey workingOrders =
                    this.workingOrders.computeIfAbsent(symbol, s -> new SourcedWorkingOrdersByUIKey());

            workingOrders.setWorkingOrder(workingOrder);
            setGTCCount(symbol, workingOrders);
        }
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder workingOrder) {

        if (OrderType.GTC == workingOrder.order.getOrderType()) {

            final String symbol = workingOrder.order.getSymbol();
            final SourcedWorkingOrdersByUIKey workingOrders = this.workingOrders.get(symbol);

            workingOrders.removeWorkingOrder(workingOrder);
            setGTCCount(symbol, workingOrders);
        }
    }

    @Override
    public void setNibblerDisconnected(final String source) {

        for (final Map.Entry<String, SourcedWorkingOrdersByUIKey> symbolOrders : workingOrders.entrySet()) {

            final SourcedWorkingOrdersByUIKey workingOrders = symbolOrders.getValue();

            if (workingOrders.clearNibblerOrders(source)) {
                final String symbol = symbolOrders.getKey();
                setGTCCount(symbol, workingOrders);
            }
        }
    }

    private void setGTCCount(final String symbol, final SourcedWorkingOrdersByUIKey workingOrders) {

        final int workingOrderCount = workingOrders.getWorkingOrders().size();
        final GTCWorkingOrderCount gtcCount = new GTCWorkingOrderCount(symbol, workingOrderCount);
        opxlWriter.setGTCWorkingOrderCount(gtcCount);
    }
}
