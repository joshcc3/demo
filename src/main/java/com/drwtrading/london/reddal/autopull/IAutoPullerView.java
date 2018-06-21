package com.drwtrading.london.reddal.autopull;

import java.util.List;
import java.util.Map;

public interface IAutoPullerView {

    void updateGlobals(final List<String> relevantSymbols, final Map<String, List<String>> symbolToWorkingPrice,
            final Map<String, List<String>> symbolToPossiblePrices);

    void displayRule(final String key, final String symbol, String side, final String orderPriceFrom, final String orderPriceTo,
            final String conditionPrice, final String conditionSide, final String qtyCondition, final String qtyThreshold,
            final boolean enabled, final String enabledByUser, final int pullCount);

    void ruleFired(final String key);

    void removeRule(final String key);

    void showMessage(final String message);
}
