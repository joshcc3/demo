package com.drwtrading.london.reddal.autopull.autopuller.msgs.updates;

public class AutoPullerRuleDeleted implements IAutoPullerUpdate {

    private final long ruleID;

    public AutoPullerRuleDeleted(final long ruleID) {
        this.ruleID = ruleID;
    }

    @Override
    public void executeOn(final IAutoPullerUpdateHandler handler) {
        handler.ruleDeleted(ruleID);
    }
}
