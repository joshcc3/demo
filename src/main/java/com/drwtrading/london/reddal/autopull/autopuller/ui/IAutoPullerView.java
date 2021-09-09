package com.drwtrading.london.reddal.autopull.autopuller.ui;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface IAutoPullerView {

    public void updateGlobals(final Collection<String> relevantSymbols, final Map<String, Set<String>> prices);

    public void displayRule(final String key, final String orderSymbol, final String mdSymbol, String side, final String orderPriceFrom,
            final String orderPriceTo, final String conditionPrice, final String conditionSide, final String qtyCondition,
            final String qtyThreshold, final boolean enabled, final String enabledByUser, final int pullCount);

    public void ruleFired(final String key);

    public void removeRule(final String key);

    public void showMessage(final String message);
}
