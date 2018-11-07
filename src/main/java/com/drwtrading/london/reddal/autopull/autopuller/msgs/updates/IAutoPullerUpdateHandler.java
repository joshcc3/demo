package com.drwtrading.london.reddal.autopull.autopuller.msgs.updates;

import com.drwtrading.london.reddal.autopull.autopuller.AutoPullerRuleState;

import java.util.List;
import java.util.Map;

public interface IAutoPullerUpdateHandler {

    public void refreshPrices(final Map<String, List<Long>> symbolPrices);

    public void setRuleState(final AutoPullerRuleState ruleState);

    public void ruleDeleted(final long ruleID);

    public void ruleFired(final AutoPullerRuleState ruleState);
}
