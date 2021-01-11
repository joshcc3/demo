package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

public class FIETFObligationState {

    private final String symbol;
    private final long millisInADay;

    private long totalMillis;
    private long totalTwoSidedMillis;

    private long lastCheckMillisSinceMidnight;
    private boolean isTwoSided;

    private int twoSidedPercentage;
    private boolean percentageChangedSinceLastViewUpdate;

    FIETFObligationState(final String symbol, final long systemOnMilliSinceMidnight, final long nowMilliSinceMidnight,
            final long maxMillisSinceMidnight) {

        this.symbol = symbol;
        this.totalMillis = nowMilliSinceMidnight - systemOnMilliSinceMidnight;
        this.millisInADay = maxMillisSinceMidnight - systemOnMilliSinceMidnight;
        this.totalTwoSidedMillis = 0;

        this.isTwoSided = false;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;

        this.twoSidedPercentage = 0;
        this.percentageChangedSinceLastViewUpdate = true;
    }

    void setState(final long nowMilliSinceMidnight, final boolean isTwoSided) {
        setState(nowMilliSinceMidnight);
        this.isTwoSided = isTwoSided;
    }

    void setState(final long nowMilliSinceMidnight) {

        final long millisSinceLastCheck = nowMilliSinceMidnight - lastCheckMillisSinceMidnight;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;

        if (this.isTwoSided) {
            totalTwoSidedMillis += millisSinceLastCheck;
        } else {
            totalMillis += millisSinceLastCheck;
        }

        recalculateTwoSidedPercentage();
    }

    private void recalculateTwoSidedPercentage() {
        final long totalTimeInTradingDay = millisInADay + totalMillis + totalTwoSidedMillis;
        final int newTwoSidedPercentage = Math.toIntExact((totalTwoSidedMillis * 2 * 100) / totalTimeInTradingDay);

        percentageChangedSinceLastViewUpdate = newTwoSidedPercentage != twoSidedPercentage;
        twoSidedPercentage = newTwoSidedPercentage;
    }

    public int getTwoSidedPercentage() {
        return twoSidedPercentage;
    }

    public boolean percentageChanged() {
        return percentageChangedSinceLastViewUpdate;
    }

    public String getSymbol() {
        return symbol;
    }
}
