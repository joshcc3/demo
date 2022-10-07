package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.utils.Constants;

import java.text.DecimalFormat;

public final class DataUtils {

    public static double normalizedQty(final long qty) {
        return qty / (double)Constants.NORMALISING_FACTOR;
    }

    static String formatPosition(final DecimalFormat formatter, final double qty) {

        final double absQty = Math.abs(qty);
        if (absQty < 10000) {
            return Integer.toString((int) qty);
        } else if (absQty < 1000000) {
            return formatter.format(qty / 1000.0) + 'K';
        } else {
            return formatter.format(qty / 1000000.0) + 'M';
        }
    }
}
