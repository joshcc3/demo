package com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds;

public class AutoPullerSafeStartRule implements IAutoPullerCmd {

    private final long ruleID;
    private final String username;

    public AutoPullerSafeStartRule(final long ruleID, final String username) {

        this.ruleID = ruleID;
        this.username = username;
    }

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {
        handler.safeStartRule(ruleID, username);
    }
}
