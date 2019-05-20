package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;

import java.util.regex.Pattern;

public class QuotingObligationState {

    private static final Pattern SPACE_REPLACE = Pattern.compile(" ", Pattern.LITERAL);

    private final String symbol;
    private final String sourceNibbler;
    private final NibblerClientHandler nibblerClient;

    private final String key;

    private long totalOffMillis;
    private long totalOnMillis;

    private int strategyID;
    private boolean isStrategyOn;
    private boolean isQuoting;
    private String stateDescription;
    private long lastCheckMillisSinceMidnight;

    QuotingObligationState(final String symbol, final int strategyID, final String sourceNibbler, final NibblerClientHandler nibblerClient,
            final long systemOnMilliSinceMidnight, final long nowMilliSinceMidnight, final boolean isStrategyOn,
            final String stateDescription) {

        this.symbol = symbol;
        this.strategyID = strategyID;
        this.sourceNibbler = sourceNibbler;

        this.key = SPACE_REPLACE.matcher(symbol + "_obligation").replaceAll("_");
        this.nibblerClient = nibblerClient;

        this.totalOffMillis = nowMilliSinceMidnight - systemOnMilliSinceMidnight;
        this.totalOnMillis = 0;

        this.isStrategyOn = isStrategyOn;
        this.isQuoting = isQuoting(isStrategyOn, stateDescription);
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

    public boolean isStrategyOn() {
        return isStrategyOn;
    }

    public boolean isQuoting() {
        return isQuoting;
    }

    public String getStateDescription() {
        return stateDescription;
    }

    public void updatePercent(final long nowMilliSinceMidnight) {
        setState(nowMilliSinceMidnight, strategyID, isStrategyOn, stateDescription);
    }

    public void setState(final long nowMilliSinceMidnight, final int strategyID, final boolean isStrategyOn, final String stateDescription) {

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

    private static boolean isQuoting(final boolean isStrategyOn, final String stateDescription) {
        return isStrategyOn && "OK".equals(stateDescription);
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
