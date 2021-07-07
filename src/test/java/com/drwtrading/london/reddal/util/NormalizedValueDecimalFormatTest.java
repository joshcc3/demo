package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.reddal.nibblers.NormalizedValueDecimalFormat;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.text.DecimalFormat;

public class NormalizedValueDecimalFormatTest {

    @Test
    public void basicTest() {
        final DecimalFormat expectedDF = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 2, 6);
        final NormalizedValueDecimalFormat df = new NormalizedValueDecimalFormat(2, 6);

        for (long i = -1000L + Integer.MIN_VALUE; i <= 1000L + Integer.MIN_VALUE; i++) {
            check(i, expectedDF, df);
        }

        for (long i = -1000L + Integer.MAX_VALUE; i <= 1000L + Integer.MAX_VALUE; i++) {
            check(i, expectedDF, df);
        }

        for (int i = -10000; i <= 10000; i++) {
            check(i * Constants.NORMALISING_FACTOR / 100, expectedDF, df);
        }

        for (long i = Long.MIN_VALUE; i <= Long.MIN_VALUE + 1000; i++) {
            check(i, expectedDF, df);
        }

        for (long i = Long.MAX_VALUE - 1000; i <= Long.MAX_VALUE - 1; i++) {
            check(i, expectedDF, df);
        }
    }

    private static void check(final long val, final DecimalFormat expectedDF, final NormalizedValueDecimalFormat actualDF) {
        final String actual = actualDF.format(val);
        final String expected = expectedDF.format(val / (double) Constants.NORMALISING_FACTOR);
        final double actualD = Double.parseDouble(actual);
        final double expectedD = Double.parseDouble(expected);
        Assert.assertEquals(actual.length(), expected.length(), actual + ", " + expected);
        Assert.assertTrue(Math.abs(actualD - expectedD) <= 0.00001);
    }

}
