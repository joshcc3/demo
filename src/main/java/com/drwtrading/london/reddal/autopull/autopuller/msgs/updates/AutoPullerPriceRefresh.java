package com.drwtrading.london.reddal.autopull.autopuller.msgs.updates;

import java.util.List;
import java.util.Map;

public class AutoPullerPriceRefresh implements IAutoPullerUpdate {

    private final Map<String, List<Long>> symbolPrices;

    public AutoPullerPriceRefresh(final Map<String, List<Long>> symbolPrices) {
        this.symbolPrices = symbolPrices;
    }

    @Override
    public void executeOn(final IAutoPullerUpdateHandler handler) {
        handler.refreshPrices(symbolPrices);
    }
}
