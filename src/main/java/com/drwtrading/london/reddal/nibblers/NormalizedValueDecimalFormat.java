package com.drwtrading.london.reddal.nibblers;

import java.util.Arrays;

public class NormalizedValueDecimalFormat {

    private static final int NORMALIZING_EXPONENT = 9;
    private static final int MAX_DIGITS = 19;
    private static final int MAX_CHARS = MAX_DIGITS + 2;
    private static final byte[] ONES = new byte[100];
    private static final byte[] TENS = new byte[100];

    static {
        for (int i = 0; i < ONES.length; i++) {
            ONES[i] = (byte) ('0' + (byte) (i % 10));
            TENS[i] = (byte) ('0' + (byte) (i / 10));
        }
    }

    private final byte[] finalBuf;
    private final byte[] tmpBuf;
    private final int minDP;
    private final int maxDP;

    public NormalizedValueDecimalFormat(final int minDP, final int maxDP) {
        this.minDP = minDP;
        this.maxDP = maxDP;
        this.finalBuf = new byte[MAX_CHARS];
        this.tmpBuf = new byte[MAX_CHARS];
        clearBuffers();
    }

    private void clearBuffers() {
        Arrays.fill(this.finalBuf, (byte) '0');
        Arrays.fill(this.tmpBuf, (byte) '0');
    }

    public String format(final long normalizedValue) {
        clearBuffers();
        final boolean isNeg = normalizedValue < 0;
        final int longStrSize = stringSize(normalizedValue);
        final int fmtStrSize;
        final int trailingZeros;

        final int decimalPoint;
        if (normalizedValue == 0) {
            fmtStrSize = 4;
            finalBuf[0] = '0';
            finalBuf[1] = '.';
            finalBuf[1] = '0';
            finalBuf[2] = '0';
            decimalPoint = 1;
            trailingZeros = 0;
        } else if (normalizedValue < 1_000_000_000 && normalizedValue > -1_000_000_000) {
            if (isNeg) {
                fmtStrSize = NORMALIZING_EXPONENT + 3;
                finalBuf[0] = '-';
                finalBuf[1] = '0';
                finalBuf[2] = '.';
                decimalPoint = 2;
            } else {
                fmtStrSize = NORMALIZING_EXPONENT + 2;
                finalBuf[0] = '0';
                finalBuf[1] = '.';
                decimalPoint = 1;

            }
            getChars(Math.abs(normalizedValue), fmtStrSize, finalBuf);
            int count = 0;
            while (count < longStrSize && finalBuf[fmtStrSize - count - 1] == '0') {
                count++;
            }
            trailingZeros = count;
        } else {
            fmtStrSize = longStrSize + 1;
            if (isNeg) {
                tmpBuf[0] = '-';
            }
            getChars(normalizedValue, longStrSize, tmpBuf);
            int count = 0;
            int finalBufCursor = fmtStrSize - 1;
            int tmpBufCursor = longStrSize - 1;
            while (count < NORMALIZING_EXPONENT && tmpBuf[tmpBufCursor] == '0') {
                count++;
                finalBufCursor--;
                tmpBufCursor--;
            }
            trailingZeros = count;
            while (count < NORMALIZING_EXPONENT) {
                final byte nextChar = tmpBuf[tmpBufCursor];
                finalBuf[finalBufCursor] = nextChar;
                count++;
                finalBufCursor--;
                tmpBufCursor--;
            }

            decimalPoint = finalBufCursor;
            finalBuf[finalBufCursor--] = '.';
            while (count < longStrSize) {
                final byte nextChar = tmpBuf[tmpBufCursor];
                finalBuf[finalBufCursor] = nextChar;
                count++;
                finalBufCursor--;
                tmpBufCursor--;
            }
        }

        final int finalFmtLen = Math.min(decimalPoint + maxDP, Math.max(decimalPoint + minDP, fmtStrSize - 1 - trailingZeros)) + 1;
        return new String(finalBuf, 0, finalFmtLen);

    }

    private static int getChars(long normalisedValue, final int index, final byte[] buf) {
        long quotient;
        int remainder;
        int charPos = index;

        final boolean isNonNeg = normalisedValue >= 0;
        if (isNonNeg) {
            normalisedValue = -normalisedValue;
        }

        while (normalisedValue <= Integer.MIN_VALUE) {
            quotient = normalisedValue / 100;
            remainder = (int) ((quotient * 100) - normalisedValue);
            normalisedValue = quotient;
            buf[--charPos] = (byte) ONES[remainder];
            buf[--charPos] = TENS[remainder];
        }

        int quotient2;
        int normValue2 = (int) normalisedValue;
        while (normValue2 <= -100) {
            quotient2 = normValue2 / 100;
            remainder = (quotient2 * 100) - normValue2;
            normValue2 = quotient2;
            buf[--charPos] = ONES[remainder];
            buf[--charPos] = TENS[remainder];
        }

        quotient2 = normValue2 / 10;
        remainder = (quotient2 * 10) - normValue2;
        buf[--charPos] = (byte) ('0' + remainder);

        if (quotient2 < 0) {
            buf[--charPos] = (byte) ('0' - quotient2);
        }
        return charPos;
    }

    static int stringSize(long normalizedValue) {
        int numMinusSign = 1;
        if (normalizedValue >= 0) {
            numMinusSign = 0;
            normalizedValue = -normalizedValue;
        }
        long pow10 = -10;
        for (int i = 1; i < MAX_DIGITS; i++) {
            if (normalizedValue > pow10) {
                return i + numMinusSign;
            }
            pow10 = 10 * pow10;
        }
        return MAX_DIGITS + numMinusSign;
    }

}
