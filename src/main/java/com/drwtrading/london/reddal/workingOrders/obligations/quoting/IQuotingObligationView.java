package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

public interface IQuotingObligationView {

    public void setRow(final String rowID, final String symbol, final String sourceNibbler, final long percentageOn,
            final boolean isStrategyOn, final boolean isStrategyQuoting, final String stateDescription, final boolean isObligationFail);

    public void deleteRow(final String id);

    public void checkWarning();
}
