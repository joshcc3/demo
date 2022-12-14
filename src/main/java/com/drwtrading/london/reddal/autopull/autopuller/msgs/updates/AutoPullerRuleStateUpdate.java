package com.drwtrading.london.reddal.autopull.autopuller.msgs.updates;

import com.drwtrading.london.reddal.autopull.autopuller.AutoPullerRuleState;

public class AutoPullerRuleStateUpdate implements IAutoPullerUpdate {

    private final AutoPullerRuleState ruleState;

    public AutoPullerRuleStateUpdate(final AutoPullerRuleState ruleState) {
        this.ruleState = ruleState;
    }

    @Override
    public void executeOn(final IAutoPullerUpdateHandler handler) {
        handler.setRuleState(ruleState);
    }
}
