package com.drwtrading.london.reddal.workingOrders.obligations;

import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;

public class NoRFQObligationsPresenter implements IRFQObligationPresenter {

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
