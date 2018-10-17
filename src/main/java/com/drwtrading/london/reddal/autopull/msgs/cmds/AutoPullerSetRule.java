package com.drwtrading.london.reddal.autopull.msgs.cmds;

import com.drwtrading.london.reddal.autopull.rules.PullRule;

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
