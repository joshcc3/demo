package com.drwtrading.london.reddal.workingOrders;

public interface IWorkingOrderView {

    void addNibbler(final String nibblerName, final boolean connected, int orderCount);

    void updateWorkingOrder(final String key, final String chainID, final String instrument, final String side, final String price,
            final int filledQuantity, final int quantity, final String state, final String orderAlgoType, final String tag,
            final String server, final boolean isDead);

    void refreshWorkingOrderCounts(final String server, int orderCount);
}
