package com.drwtrading.london.reddal.stacks.strategiesUI;

import java.util.Collection;

public interface IStackStrategiesUI {

    public void noInstID(final String type);

    public void addInstType(final Collection<String> instTypes);

    public void addAvailableNibblers(final Collection<String> nibblers);

    public void setInstID(final String type, final String isin, final String ccy, final String mic);

    public void removeAll(final String nibblerName);

    public void setRow(final String nibblerName, final long id, final String quoteSymbol, final String quoteISIN, final String quoteCCY,
                       final String quoteMIC, final String leanInstType, final String leanSymbol, final String leanISIN, final String leanCCY,
                       final String leanMIC, final boolean isQuoteInstDefEventAvailable, final boolean isQuoteBookAvailable,
                       final boolean isLeanBookAvailable, final boolean isFXAvailable, final boolean isAdditiveAvailable,
                       final String selectedConfigType, String additiveSymbol);
}
