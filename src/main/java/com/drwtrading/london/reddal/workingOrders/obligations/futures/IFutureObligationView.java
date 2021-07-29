package com.drwtrading.london.reddal.workingOrders.obligations.futures;

import com.drwtrading.london.reddal.opxl.QuotingObligationType;

public interface IFutureObligationView {

    public void setObligation(final String symbol, final QuotingObligationType obligationType, final String bpsObligation, final String qtyObligation, final boolean obligationMet,
            final String bpsWide, final String qtyShowing);
}
