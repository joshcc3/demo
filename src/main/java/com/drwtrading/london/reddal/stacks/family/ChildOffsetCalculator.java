package com.drwtrading.london.reddal.stacks.family;

import java.util.HashMap;
import java.util.Map;

public final class ChildOffsetCalculator {

    private static final double DEFAULT_EXCHANGE_OFFSETS = 5d;
    private static final Map<String, Double> EXCHANGE_OFFSETS;

    static {
        EXCHANGE_OFFSETS = new HashMap<>();

        EXCHANGE_OFFSETS.put("AV", 5.5d);
        EXCHANGE_OFFSETS.put("BB", 1.6d);
        EXCHANGE_OFFSETS.put("DC", 2.05d);
        EXCHANGE_OFFSETS.put("EB", 3d);
        EXCHANGE_OFFSETS.put("FH", 2.05d);
        EXCHANGE_OFFSETS.put("FP", 1.6d);
        EXCHANGE_OFFSETS.put("GY", 1.6d);
        EXCHANGE_OFFSETS.put("ID", 1.75d);
        EXCHANGE_OFFSETS.put("IM", 1.6d);
        EXCHANGE_OFFSETS.put("IX", 1.25d);
        EXCHANGE_OFFSETS.put("LN", 1.6d);
        EXCHANGE_OFFSETS.put("NA", 1.6d);
        EXCHANGE_OFFSETS.put("NO", 2.45d);
        EXCHANGE_OFFSETS.put("RFQ", 0.0d);
        EXCHANGE_OFFSETS.put("PA", 1.6d);
        EXCHANGE_OFFSETS.put("PL", 1.6d);
        EXCHANGE_OFFSETS.put("SE", 3d);
        EXCHANGE_OFFSETS.put("SS", 2.05d);
        EXCHANGE_OFFSETS.put("UF", 1.25d);
        EXCHANGE_OFFSETS.put("UW", 1.25d);
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
