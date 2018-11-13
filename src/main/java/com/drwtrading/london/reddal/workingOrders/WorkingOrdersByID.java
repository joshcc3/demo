package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;

public class WorkingOrdersByID {

    private final LongMap<Long> prevOrderIDs;
    private final LongMap<SourcedWorkingOrder> workingOrdersByID;

    public WorkingOrdersByID() {

        this.prevOrderIDs = new LongMap<>();
        this.workingOrdersByID = new LongMap<>();
    }

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        final Long prevOrderID = prevOrderIDs.put(sourcedOrder.order.getWorkingOrderID(), sourcedOrder.order.getBookOrderID());
        if (null != prevOrderID) {
            workingOrdersByID.remove(prevOrderID);
        }

        workingOrdersByID.put(sourcedOrder.order.getBookOrderID(), sourcedOrder);
    }

    public void removeWorkingOrder(final SourcedWorkingOrder sourcedOrder) {

        prevOrderIDs.remove(sourcedOrder.order.getWorkingOrderID());
        workingOrdersByID.remove(sourcedOrder.order.getBookOrderID());
    }

    public SourcedWorkingOrder getWorkingOrder(final long orderID) {

        return workingOrdersByID.get(orderID);
    }

    public Iterable<LongMapNode<SourcedWorkingOrder>> getWorkingOrders() {
        return workingOrdersByID;
    }
}
