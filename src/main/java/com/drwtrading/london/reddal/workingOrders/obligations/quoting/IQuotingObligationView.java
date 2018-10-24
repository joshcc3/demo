package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

public interface IQuotingObligationView {

    public void setRow(final String rowID, final String symbol, final int percentageOn, final boolean isStrategyOn,
            final boolean isObligationFail);

    public void deleteRow(final String id);

    public void checkWarning();
}
