package com.drwtrading.london.reddal.workingOrders.obligations.futures;

import com.drwtrading.photons.eeif.configuration.QuotingObligation;

public class FutureObligationPerformance {

    private final QuotingObligation obligation;

    private final boolean isObligationMet;
    private final double bpsWide;
    private final long qtyShowing;

    FutureObligationPerformance(final QuotingObligation obligation, final boolean isObligationMet, final double bpsWide,
            final long qtyShowing) {

        this.obligation = obligation;
        this.isObligationMet = isObligationMet;
        this.bpsWide = bpsWide;
        this.qtyShowing = qtyShowing;
    }

    public QuotingObligation getObligation() {
        return obligation;
    }

    public boolean isObligationMet() {
        return isObligationMet;
    }

    public double getBpsWide() {
        return bpsWide;
    }

    public long getQtyShowing() {
        return qtyShowing;
    }
}
