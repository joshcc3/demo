package com.drwtrading.london.reddal.workingOrders;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class SourcedWorkingOrdersByUIKey {

    private final Map<String, SourcedWorkingOrder> workingOrders;

    public SourcedWorkingOrdersByUIKey() {

        this.workingOrders = new HashMap<>();
    }

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {
        workingOrders.put(sourcedOrder.uiKey, sourcedOrder);
    }

    public void removeWorkingOrder(final SourcedWorkingOrder sourcedOrder) {
        workingOrders.remove(sourcedOrder.uiKey);
    }

    public boolean clearNibblerOrders(final String sourceNibbler) {

        final Iterator<SourcedWorkingOrder> workingOrderIterator = workingOrders.values().iterator();

        boolean result = false;
        while (workingOrderIterator.hasNext()) {

            final SourcedWorkingOrder sourcedOrder = workingOrderIterator.next();
            if (sourceNibbler.equals(sourcedOrder.source)) {
                workingOrderIterator.remove();
                result = true;
            }
        }

        return result;
    }

    public Collection<SourcedWorkingOrder> getWorkingOrders() {
        return workingOrders.values();
    }
}
