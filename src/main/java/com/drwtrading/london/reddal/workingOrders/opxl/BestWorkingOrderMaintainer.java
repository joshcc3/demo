package com.drwtrading.london.reddal.workingOrders.opxl;

import com.drwtrading.london.reddal.workingOrders.IWorkingOrdersCallback;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByBestPrice;

import java.util.HashMap;
import java.util.Map;

public class BestWorkingOrderMaintainer implements IWorkingOrdersCallback {

    private final OPXLBestWorkingOrdersPresenter opxlWriter;

    private final Map<String, WorkingOrdersByBestPrice> workingOrders;

    public BestWorkingOrderMaintainer(final OPXLBestWorkingOrdersPresenter opxlWriter) {

        this.opxlWriter = opxlWriter;

        this.workingOrders = new HashMap<>();
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final WorkingOrdersByBestPrice orderedWorkingOrders = getWorkingOrdersByBestPrice(workingOrder.order.getSymbol());
        final Long prevPrice = orderedWorkingOrders.setWorkingOrder(workingOrder);

        final long bestPrice = orderedWorkingOrders.getBestPrice(workingOrder.order.getSide());

        if (bestPrice == workingOrder.order.getPrice() || (null != prevPrice && bestPrice == prevPrice)) {
            setBestWorkingPrice(orderedWorkingOrders);
        }
    }

    private WorkingOrdersByBestPrice getWorkingOrdersByBestPrice(final String symbol) {

        final WorkingOrdersByBestPrice orderedWorkingOrders = workingOrders.get(symbol);

        if (null == orderedWorkingOrders) {
            final WorkingOrdersByBestPrice result = new WorkingOrdersByBestPrice(symbol);
            workingOrders.put(symbol, result);
            return result;
        } else {
            return orderedWorkingOrders;
        }
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder workingOrder) {

        final WorkingOrdersByBestPrice orderedWorkingOrders = workingOrders.get(workingOrder.order.getSymbol());
        final long bestPrice = orderedWorkingOrders.getBestPrice(workingOrder.order.getSide());

        final long oldPrice = orderedWorkingOrders.removeWorkingOrder(workingOrder);

        if (bestPrice == oldPrice) {
            setBestWorkingPrice(orderedWorkingOrders);
        }
    }

    @Override
    public void setNibblerDisconnected(final String source) {

        for (final WorkingOrdersByBestPrice orderedWorkingOrders : workingOrders.values()) {

            if (orderedWorkingOrders.connectionLost(source)) {
                setBestWorkingPrice(orderedWorkingOrders);
            }
        }
    }

    private void setBestWorkingPrice(final WorkingOrdersByBestPrice orderedWorkingOrders) {

        final BestWorkingPriceForSymbol bestWorkingPrice = orderedWorkingOrders.getBestWorkingPrices();
        opxlWriter.setTopOfBigWorkingOrders(bestWorkingPrice);
    }
}
