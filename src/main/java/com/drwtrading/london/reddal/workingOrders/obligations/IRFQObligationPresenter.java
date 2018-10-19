package com.drwtrading.london.reddal.workingOrders.obligations;

import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;

public interface IRFQObligationPresenter {

    public void setWorkingOrder(final SourcedWorkingOrder sourcedOrder);

    public void deleteWorkingOrder(final SourcedWorkingOrder sourcedOrder);

    public void setNibblerDisconnected(final String source);
}
