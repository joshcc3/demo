package com.drwtrading.london.reddal.workingOrders.obligations.futures;

public interface IFutureObligationView {

    public void setObligation(final String symbol, final String bpsObligation, final String qtyObligation, final boolean obligationMet,
            final String bpsWide, final String qtyShowing);
}
