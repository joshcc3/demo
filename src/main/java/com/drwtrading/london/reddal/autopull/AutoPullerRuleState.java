package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.reddal.autopull.rules.PullRule;

import java.util.Objects;

public class AutoPullerRuleState {

    public final PullRule rule;
    public final MDSource owningMDSource;
    public final boolean isEnabled;
    public final String associatedUser;
    public final int matchedOrders;

    public AutoPullerRuleState(final PullRule rule, final MDSource owningMDSource, final boolean isEnabled, final String associatedUser,
            final int matchedOrders) {

        this.rule = rule;
        this.owningMDSource = owningMDSource;

        this.isEnabled = isEnabled;
        this.associatedUser = associatedUser;
        this.matchedOrders = matchedOrders;
    }

    @Override
    public boolean equals(final Object o) {

        if (this == o) {
            return true;
        } else if (null == o || getClass() != o.getClass()) {
            return false;
        } else {
            final AutoPullerRuleState ruleState = (AutoPullerRuleState) o;
            return isEnabled == ruleState.isEnabled && matchedOrders == ruleState.matchedOrders &&
                    owningMDSource == ruleState.owningMDSource && Objects.equals(associatedUser, ruleState.associatedUser) &&
                    rule.equals(ruleState.rule);
        }
    }

    @Override
    public int hashCode() {
        return Long.hashCode(rule.ruleID);
    }
}
