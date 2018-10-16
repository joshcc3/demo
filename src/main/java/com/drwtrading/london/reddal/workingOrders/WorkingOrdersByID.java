package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;

public class WorkingOrdersByID {

    private final LongMap<Long> prevOrderIDs;
    private final LongMap<WorkingOrder> workingOrdersByID;

    public WorkingOrdersByID() {

        this.prevOrderIDs = new LongMap<>();
        this.workingOrdersByID = new LongMap<>();
    }

    public void setWorkingOrder(final WorkingOrder workingOrder) {

        final Long prevOrderID = prevOrderIDs.put(workingOrder.getWorkingOrderID(), workingOrder.getBookOrderID());
        if (null != prevOrderID) {
            workingOrdersByID.remove(prevOrderID);
        }

        workingOrdersByID.put(workingOrder.getBookOrderID(), workingOrder);
    }

    public void removeWorkingOrder(final WorkingOrder workingOrder) {

        prevOrderIDs.remove(workingOrder.getWorkingOrderID());
        workingOrdersByID.remove(workingOrder.getBookOrderID());
    }

    public WorkingOrder getWorkingOrder(final long orderID) {

        return workingOrdersByID.get(orderID);
    }

    public Iterable<LongMapNode<WorkingOrder>> getWorkingOrders() {
        return workingOrdersByID;
    }
}
