package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

import com.drwtrading.photons.eeif.configuration.QuotingObligation;

public class FIETFObligationState {

    private final QuotingObligation obligation;

    private long totalMillis;
    private long totalTwoSidedMillis;

    private long lastCheckMillisSinceMidnight;
    private boolean isTwoSided;

    FIETFObligationState(final QuotingObligation obligation, final long systemOnMilliSinceMidnight, final long nowMilliSinceMidnight) {

        this.obligation = obligation;
        this.totalMillis = nowMilliSinceMidnight - systemOnMilliSinceMidnight;
        this.totalTwoSidedMillis = 0;

        this.isTwoSided = false;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;
    }

    void setState(final long nowMilliSinceMidnight, final boolean isTwoSided) {

        final long millisSinceLastCheck = nowMilliSinceMidnight - lastCheckMillisSinceMidnight;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;

        if (this.isTwoSided) {
            totalTwoSidedMillis += millisSinceLastCheck;
        } else {
            totalMillis += millisSinceLastCheck;
        }

        this.isTwoSided = isTwoSided;
    }

    long getTotalTime() {
        return totalMillis + totalTwoSidedMillis;
    }

    long getMillisTwoSided() {
        return totalTwoSidedMillis;
    }

    long getTwoSidedPercent() {
        final long totalTime = totalTwoSidedMillis + totalMillis;
        if (0 < totalTime) {
            return (100 * totalTwoSidedMillis) / totalTime;
        } else {
            return 0;
        }
    }

    QuotingObligation getObligation() {
        return this.obligation;
    }
}
