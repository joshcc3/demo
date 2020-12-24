package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;

import java.util.regex.Pattern;

public class FIETFObligationState {

    private static final Pattern SPACE_REPLACE = Pattern.compile(" ", Pattern.LITERAL);

    private final String symbol;
    private final String sourceNibbler;
    private final NibblerTransportOrderEntry nibblerClient;

    private final String key;

    private long totalOffMillis;
    private long totalOnMillis;

    private boolean isAvailable;
    private int strategyID;
    private boolean isEnabled;
    private boolean isStrategyOn;
    private boolean isQuoting;
    private String stateDescription;
    private long lastCheckMillisSinceMidnight;

    FIETFObligationState(final String symbol, final int strategyID, final String sourceNibbler,
            final NibblerTransportOrderEntry nibblerClient, final long systemOnMilliSinceMidnight, final long nowMilliSinceMidnight,
            final boolean isStrategyOn, final String stateDescription) {

        this.symbol = symbol;
        this.strategyID = strategyID;
        this.sourceNibbler = sourceNibbler;

        this.key = SPACE_REPLACE.matcher(symbol + "_obligation").replaceAll("_");
        this.nibblerClient = nibblerClient;

        this.totalOffMillis = nowMilliSinceMidnight - systemOnMilliSinceMidnight;
        this.totalOnMillis = 0;

        this.isAvailable = true;
        this.isEnabled = true;
        this.isStrategyOn = isStrategyOn;
        this.isQuoting = isQuoting(isStrategyOn, stateDescription);
        this.stateDescription = stateDescription;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;
    }

    public String getSymbol() {
        return symbol;
    }

    boolean isAvailable() {
        return isAvailable;
    }

    int getStrategyID() {
        return strategyID;
    }

    public String getSourceNibbler() {
        return sourceNibbler;
    }

    NibblerTransportOrderEntry getNibblerOE() {
        return nibblerClient;
    }

    public String getKey() {
        return key;
    }

    boolean isStrategyOn() {
        return isStrategyOn;
    }

    public boolean isQuoting() {
        return isQuoting;
    }

    String getStateDescription() {
        return stateDescription;
    }

    void setIsAvailable(final boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    void updatePercent(final long nowMilliSinceMidnight) {
        setState(nowMilliSinceMidnight, strategyID, isStrategyOn, stateDescription);
    }

    boolean isEnabled() {
        return isEnabled;
    }

    void setEnabled(final boolean enabled) {
        this.isEnabled = enabled;
    }

    void setState(final long nowMilliSinceMidnight, final int strategyID, final boolean isStrategyOn, final String stateDescription) {

        final long millisSinceLastCheck = nowMilliSinceMidnight - lastCheckMillisSinceMidnight;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;

        this.strategyID = strategyID;

        if (this.isQuoting) {
            totalOnMillis += millisSinceLastCheck;
        } else {
            totalOffMillis += millisSinceLastCheck;
        }

        this.isStrategyOn = isStrategyOn;
        this.isQuoting = isQuoting(isStrategyOn, stateDescription);
        this.stateDescription = stateDescription;
    }

    long getTotalTime() {
        return totalOffMillis + totalOnMillis;
    }

    private static boolean isQuoting(final boolean isStrategyOn, final String stateDescription) {
        return isStrategyOn && "OK".equals(stateDescription);
    }

    long getMillisOn() {
        return totalOnMillis;
    }

    long getOnPercent() {
        final long totalTime = totalOnMillis + totalOffMillis;
        if (0 < totalTime) {
            return (100 * totalOnMillis) / totalTime;
        } else {
            return 0;
        }
    }
}
