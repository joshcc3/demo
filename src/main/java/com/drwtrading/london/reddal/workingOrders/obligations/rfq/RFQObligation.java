package com.drwtrading.london.reddal.workingOrders.obligations.rfq;

import java.util.List;

public class RFQObligation {

    public final String symbol;
    public final List<RFQObligationValue> obligations;

    RFQObligation(final String symbol, final List<RFQObligationValue> obligations) {
        this.symbol = symbol;
        this.obligations = obligations;
    }
}
