package com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds;

import com.drwtrading.london.eeif.utils.application.User;

public class AutoPullerSafeStartRule implements IAutoPullerCmd {

    private final long ruleID;
    private final User user;

    public AutoPullerSafeStartRule(final long ruleID, final User user) {

        this.ruleID = ruleID;
        this.user = user;
    }

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {
        handler.safeStartRule(ruleID, user);
    }
}
