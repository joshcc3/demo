package com.drwtrading.london.reddal.workingOrders.ui;

public interface IWorkingOrderView {

    public void addNibbler(final String nibblerName, final boolean connected, int orderCount);

    public void setWorkingOrder(final String key, final String chainID, final String instrument, final String side, final String price,
            final long filledQuantity, final long quantity, final String orderAlgoType, final String tag, final String server);

    public void deleteWorkingOrder(final String key);

    public void refreshWorkingOrderCounts(final String server, int orderCount);

    public void addLoggedInUser(final String username);
}
