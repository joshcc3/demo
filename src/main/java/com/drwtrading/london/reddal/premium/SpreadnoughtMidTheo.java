package com.drwtrading.london.reddal.premium;

class SpreadnoughtMidTheo {

    final String symbol;

    private boolean isValid;
    private long mid;

    SpreadnoughtMidTheo(final String symbol, final boolean isValid, final long mid) {

        this.symbol = symbol;
        this.isValid = isValid;
        this.mid = mid;
    }

    void set(final boolean isValid, final long mid) {

        this.isValid = isValid;
        this.mid = mid;
    }

    boolean isValid() {
        return isValid;
    }

    long getMid() {
        return mid;
    }
}
