package com.drwtrading.london.reddal.workingOrders;

public interface IWorkingOrdersCallback {

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder);

    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder);

    public void setNibblerDisconnected(final String source);
}
