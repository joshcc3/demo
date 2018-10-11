package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.collections.LongMap;

class WorkingOrders {

    private final LongMap<Long> prevOrderIDs;
    private final LongMap<WorkingOrder> workingOrdersByID;

    WorkingOrders() {

        this.prevOrderIDs = new LongMap<>();
        this.workingOrdersByID = new LongMap<>();
    }

    void setWorkingOrder(final WorkingOrder workingOrder) {

        final Long prevOrderID = prevOrderIDs.put(workingOrder.getWorkingOrderID(), workingOrder.getBookOrderID());
        if (null != prevOrderID) {
            workingOrdersByID.remove(prevOrderID);
        }

        workingOrdersByID.put(workingOrder.getBookOrderID(), workingOrder);
    }

    void removeWorkingOrder(final WorkingOrder workingOrder) {

        prevOrderIDs.remove(workingOrder.getWorkingOrderID());
        workingOrdersByID.remove(workingOrder.getBookOrderID());
    }

    WorkingOrder getWorkingOrder(final long orderID) {

        return workingOrdersByID.get(orderID);
    }
}
