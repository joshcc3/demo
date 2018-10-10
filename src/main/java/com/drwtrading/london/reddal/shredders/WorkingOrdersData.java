package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.collections.LongMap;

public class WorkingOrdersData {

    private final LongMap<LongMap<WorkingOrder>> workingOrdersByPrice;

    public WorkingOrdersData() {

        this.workingOrdersByPrice = new LongMap<>();
    }

    public void setWorkingOrder(final WorkingOrder workingOrder) {

        final LongMap<WorkingOrder> ordersByBookID = workingOrdersByPrice.get(workingOrder.getPrice());

        if (null == ordersByBookID) {

            final LongMap<WorkingOrder> newOrders = new LongMap<>();
            workingOrdersByPrice.put(workingOrder.getPrice(), newOrders);
            newOrders.put(workingOrder.getBookOrderID(), workingOrder);
        } else {

            ordersByBookID.put(workingOrder.getBookOrderID(), workingOrder);
        }
    }

    public void removeWorkingOrder(final WorkingOrder workingOrder) {

        final LongMap<WorkingOrder> ordersByBookID = workingOrdersByPrice.get(workingOrder.getPrice());

        if (null != ordersByBookID) {
            ordersByBookID.remove(workingOrder.getBookOrderID());
            if (ordersByBookID.isEmpty()) {
                workingOrdersByPrice.remove(workingOrder.getPrice());
            }
        }
    }
}
