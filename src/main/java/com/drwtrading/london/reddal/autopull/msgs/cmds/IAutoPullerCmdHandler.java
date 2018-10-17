package com.drwtrading.london.reddal.autopull.msgs.cmds;

import com.drwtrading.london.reddal.autopull.rules.PullRule;

public interface IAutoPullerCmdHandler {

    public void priceRefreshRequest();

    public void setRule(final PullRule pullRule);

    public void deleteRule(final long ruleID);

    public void safeStartAll(final String username);

    public void safeStartRule(final long ruleID, final String username);

    public void stopAll();

    public void stopRule(final long ruleID);
}
