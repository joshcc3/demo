package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.reddal.orderManagement.oe.UpdateFromServer;

import java.util.Collection;
import java.util.Map;

public interface IOrdersView {

    public void orders(final Collection<Map<String, String>> workingOrderUpdates);

    public void managedOrders(final Collection<UpdateFromServer> stringUpdateFromServerMap);
}
