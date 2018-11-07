package com.drwtrading.london.reddal.autopull.autopuller.msgs.cmds;

import com.drwtrading.london.reddal.autopull.autopuller.rules.PullRule;

public class AutoPullerSetRule implements IAutoPullerCmd {

    private final PullRule pullRule;

    public AutoPullerSetRule(final PullRule pullRule) {

        this.pullRule = pullRule;
    }

    @Override
    public void executeOn(final IAutoPullerCmdHandler handler) {
        handler.setRule(pullRule);
    }
}
