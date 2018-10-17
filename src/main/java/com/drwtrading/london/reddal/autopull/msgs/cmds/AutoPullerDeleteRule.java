package com.drwtrading.london.reddal.autopull.msgs.cmds;

public class AutoPullerDeleteRule implements IAutoPullerCmd {

    private final long ruleID;

    public AutoPullerDeleteRule(final long ruleID) {
        this.ruleID = ruleID;
    }

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {
        handler.deleteRule(ruleID);
    }
}
