package com.drwtrading.london.reddal.stacks.strategiesUI;

import java.util.Collection;

public interface IStackStrategiesUI {

    public void noInstID(final String type);

    public void addInstType(final Collection<String> instTypes);

    public void setInstID(final String type, final String isin, final String ccy, final String mic);

    public void removeAll();

    public void setRow(final long id, final String quoteSymbol, final String quoteISIN, final String quoteCCY, final String quoteMIC,
            final String leanSymbol, final String leanISIN, final String leanCCY, final String leanMIC,
            final boolean isQuoteInstDefEventAvailable, final boolean isQuoteBookAvailable, final boolean isLeanBookAvailable,
            final boolean isFXAvailable, final String selectedConfigType);
}
