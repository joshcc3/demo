package com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds;

import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.reddal.autopull.autopuller.rules.PullRule;

public interface IAutoPullerCmdHandler {

    public void priceRefreshRequest();

    public void setRule(final PullRule pullRule);

    public void deleteRule(final long ruleID);

    public void safeStartAll(final User user);

    public void safeStartRule(final long ruleID, final User user);

    public void stopAll();

    public void stopRule(final long ruleID);
}
