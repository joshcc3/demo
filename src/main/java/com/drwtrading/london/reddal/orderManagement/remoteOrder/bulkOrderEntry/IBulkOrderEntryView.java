package com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry;

public interface IBulkOrderEntryView {

    public void printError(final String msg);

    public void addOrder(final String symbol, final String side, final String qty);

    public void addPricedOrder(final String symbol, final String side, final String price, final String qty);

    public void clearOrders();
}
