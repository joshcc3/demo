package com.drwtrading.london.reddal.stacks.family;

import java.util.HashMap;
import java.util.Map;

public final class ChildOffsetCalculator {

    private static final double DEFAULT_EXCHANGE_OFFSETS = 5d;
    private static final Map<String, Double> EXCHANGE_OFFSETS;

    static {
        EXCHANGE_OFFSETS = new HashMap<>();

        EXCHANGE_OFFSETS.put("AV", 5d);
        EXCHANGE_OFFSETS.put("BB", 1.1d);
        EXCHANGE_OFFSETS.put("DC", 1.55d);
        EXCHANGE_OFFSETS.put("EB", 3d);
        EXCHANGE_OFFSETS.put("FH", 1.55d);
        EXCHANGE_OFFSETS.put("GY", 1.3d);
        EXCHANGE_OFFSETS.put("ID", 1.25d);
        EXCHANGE_OFFSETS.put("IM", 3d);
        EXCHANGE_OFFSETS.put("IX", 0.75d);
        EXCHANGE_OFFSETS.put("LN", 1.5d);
        EXCHANGE_OFFSETS.put("NA", 1.1d);
        EXCHANGE_OFFSETS.put("NO", 1.95d);
        EXCHANGE_OFFSETS.put("RFQ", 0d);
        EXCHANGE_OFFSETS.put("PA", 1.1d);
        EXCHANGE_OFFSETS.put("PL", 1.1d);
        EXCHANGE_OFFSETS.put("SE", 3d);
        EXCHANGE_OFFSETS.put("SS", 1.55d);
        EXCHANGE_OFFSETS.put("UF", 0.75d);
        EXCHANGE_OFFSETS.put("UW", 0.75d);
    }

    static double getSymbolOffset(final String symbol) {

        final String[] parts = symbol.split(" ");
        if (1 < parts.length) {

            final String suffix = parts[1];
            final Double exchangeOffset = EXCHANGE_OFFSETS.get(suffix);
            if (null == exchangeOffset) {
                return DEFAULT_EXCHANGE_OFFSETS;
            } else {
                return exchangeOffset;
            }
        } else {
            return DEFAULT_EXCHANGE_OFFSETS;
        }
    }
}
