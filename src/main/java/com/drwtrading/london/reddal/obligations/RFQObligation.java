package com.drwtrading.london.reddal.obligations;

import java.util.List;

public class RFQObligation {
    public final String symbol;
    public final List<Obligation> obligations;

    public RFQObligation(String symbol, List<Obligation> obligations) {
        this.symbol = symbol;
        this.obligations = obligations;
    }
}
