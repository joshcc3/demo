package com.drwtrading.london.reddal.orderManagement.remoteOrder.ui;

public interface IBulkOrderEntryView {

    public void printError(final String msg);

    public void addOrder(final String symbol, final String side, final String qty);

    public void clearOrders();
}
