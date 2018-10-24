package com.drwtrading.london.reddal.workingOrders.obligations.lse;

import java.util.regex.Pattern;

public class QuotingObligationState {

    private static final Pattern SPACE_REPLACE = Pattern.compile(" ", Pattern.LITERAL);
    private final String symbol;
    private final String key;

    private long totalOffMillis;
    private long totalOnMillis;

    private boolean isOn;
    private long lastCheckMillisSinceMidnight;

    QuotingObligationState(final String symbol, final long systemOnMilliSinceMidnight, final long nowMilliSinceMidnight, final boolean isOn) {

        this.symbol = symbol;
        this.key = SPACE_REPLACE.matcher(symbol + "_obligation").replaceAll("_");

        this.totalOffMillis = nowMilliSinceMidnight - systemOnMilliSinceMidnight;
        this.totalOnMillis = 0;

        this.isOn = isOn;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;
    }

    public String getSymbol() {
        return symbol;
    }

    public String getKey() {
        return key;
    }

    public boolean isOn() {
        return isOn;
    }

    public void setState(final long nowMilliSinceMidnight, final boolean isOn) {

        final long millisSinceLastCheck = nowMilliSinceMidnight - lastCheckMillisSinceMidnight;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;

        if (this.isOn) {
            totalOnMillis += millisSinceLastCheck;
        } else {
            totalOffMillis += millisSinceLastCheck;
        }

        this.isOn = isOn;
    }

    public void updatePercent(final long nowMilliSinceMidnight) {
        setState(nowMilliSinceMidnight, isOn);
    }

    public long getOnPercent() {
        final long totalTime = totalOnMillis + totalOffMillis;
        if (0 < totalTime) {
            return (100 * totalOnMillis) / totalTime;
        } else {
            return 0;
        }
    }
}
