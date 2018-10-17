package com.drwtrading.london.reddal.autopull.msgs.cmds;

public class AutoPullerStopRule implements IAutoPullerCmd {

    private final long ruleID;

    public AutoPullerStopRule(final long ruleID) {
        this.ruleID = ruleID;
    }

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {
        handler.stopRule(ruleID);
    }
}
