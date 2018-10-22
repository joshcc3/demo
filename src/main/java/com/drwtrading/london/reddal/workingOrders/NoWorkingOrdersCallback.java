package com.drwtrading.london.reddal.workingOrders;

public class NoWorkingOrdersCallback implements IWorkingOrdersCallback {

    public static final NoWorkingOrdersCallback INSTANCE = new NoWorkingOrdersCallback();

    private NoWorkingOrdersCallback() {
        // singleton
    }

    @Override
    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder) {
        // no-op
    }

    @Override
    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder) {
        // no-op
    }

    @Override
    public void setNibblerDisconnected(final String source) {
        // no-op
    }
}
