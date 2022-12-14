package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.staticData.CCY;

import java.util.EnumMap;
import java.util.Map;

public class Mathematics {

    private static final Map<CCY, Double> TO_EUR_RATES = new EnumMap<>(CCY.class);

    static {
        TO_EUR_RATES.put(CCY.CAD, 0.6742);
        TO_EUR_RATES.put(CCY.CHF, 0.809946);
        TO_EUR_RATES.put(CCY.CZK, 0.036515);
        TO_EUR_RATES.put(CCY.DKK, 0.134013);
        TO_EUR_RATES.put(CCY.EUR, 1.0);
        TO_EUR_RATES.put(CCY.GBP, 1.206731);
        TO_EUR_RATES.put(CCY.JPY, 0.00701);
        TO_EUR_RATES.put(CCY.NOK, 0.118807);
        TO_EUR_RATES.put(CCY.PLN, 0.232775);
        TO_EUR_RATES.put(CCY.RUB, 0.018);
        TO_EUR_RATES.put(CCY.SEK, 0.112606);
        TO_EUR_RATES.put(CCY.SGD, 0.64);
        TO_EUR_RATES.put(CCY.USD, 0.7332);
        TO_EUR_RATES.put(CCY.ZAR, 0.0616423);

        TO_EUR_RATES.put(CCY.GBX, TO_EUR_RATES.get(CCY.GBP) / 100);
        TO_EUR_RATES.put(CCY.ZAC, TO_EUR_RATES.get(CCY.ZAR) / 100);
    }

    public static double toQuantityFromNotionalInSafetyCurrency(final double notionalInSafetyCurrency, final long price,
            final IBook<?> book, final double wpv) {

        final Double fxRate = TO_EUR_RATES.get(book.getCCY());

        if (0 == price || null == fxRate) {
            return 0;
        } else {
            final double priceLocal = price / (double) Constants.NORMALISING_FACTOR;
            final double priceEUR = fxRate * priceLocal;
            final double notionalValue = wpv * priceEUR;
            return notionalInSafetyCurrency / notionalValue;
        }
    }
}
