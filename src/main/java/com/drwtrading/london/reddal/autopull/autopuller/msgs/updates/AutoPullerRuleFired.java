package com.drwtrading.london.reddal.autopull.autopuller.msgs.updates;

import com.drwtrading.london.reddal.autopull.autopuller.AutoPullerRuleState;

public class AutoPullerRuleFired implements IAutoPullerUpdate {

    private final AutoPullerRuleState ruleState;

    public AutoPullerRuleFired(final AutoPullerRuleState ruleState) {
        this.ruleState = ruleState;
    }

    @Override
    public void executeOn(final IAutoPullerUpdateHandler handler) {
        handler.ruleFired(ruleState);
    }
}
