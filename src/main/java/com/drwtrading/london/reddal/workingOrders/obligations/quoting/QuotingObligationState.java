package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;

import java.util.regex.Pattern;

public class QuotingObligationState {

    private static final Pattern SPACE_REPLACE = Pattern.compile(" ", Pattern.LITERAL);

    private final String symbol;
    private final int strategyID;
    private final String sourceNibbler;
    private final NibblerClientHandler nibblerClient;

    private final String key;

    private long totalOffMillis;
    private long totalOnMillis;

    private boolean isOn;
    private String stateDescription;
    private long lastCheckMillisSinceMidnight;

    QuotingObligationState(final String symbol, final int strategyID, final String sourceNibbler, final NibblerClientHandler nibblerClient,
            final long systemOnMilliSinceMidnight, final long nowMilliSinceMidnight, final boolean isOn, final String stateDescription) {

        this.symbol = symbol;
        this.strategyID = strategyID;
        this.sourceNibbler = sourceNibbler;

        this.key = SPACE_REPLACE.matcher(symbol + "_obligation").replaceAll("_");
        this.nibblerClient = nibblerClient;

        this.totalOffMillis = nowMilliSinceMidnight - systemOnMilliSinceMidnight;
        this.totalOnMillis = 0;

        this.isOn = isOn;
        this.stateDescription = stateDescription;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;
    }

    public String getSymbol() {
        return symbol;
    }

    public int getStrategyID() {
        return strategyID;
    }

    public String getSourceNibbler() {
        return sourceNibbler;
    }

    public NibblerClientHandler getNibblerClient() {
        return nibblerClient;
    }

    public String getKey() {
        return key;
    }

    public boolean isOn() {
        return isOn;
    }

    public String getStateDescription() {
        return stateDescription;
    }

    public void setState(final long nowMilliSinceMidnight, final boolean isOn, final String stateDescription) {

        final long millisSinceLastCheck = nowMilliSinceMidnight - lastCheckMillisSinceMidnight;
        this.lastCheckMillisSinceMidnight = nowMilliSinceMidnight;

        if (this.isOn) {
            totalOnMillis += millisSinceLastCheck;
        } else {
            totalOffMillis += millisSinceLastCheck;
        }

        this.isOn = isOn;
        this.stateDescription = stateDescription;
    }

    public void updatePercent(final long nowMilliSinceMidnight) {
        setState(nowMilliSinceMidnight, isOn, stateDescription);
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
