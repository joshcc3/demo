package com.drwtrading.london.reddal.autopull.ui;

import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface IAutoPullerView {

    public void updateGlobals(final Collection<String> relevantSymbols, final Map<String, List<String>> symbolToWorkingPrice,
            final Map<String, List<String>> symbolToPossiblePrices);

    public void displayRule(final String key, final String symbol, String side, final String orderPriceFrom, final String orderPriceTo,
            final String conditionPrice, final String conditionSide, final String qtyCondition, final String qtyThreshold,
            final boolean enabled, final String enabledByUser, final int pullCount);

    public void ruleFired(final String key);

    public void removeRule(final String key);

    public void showMessage(final String message);
}
