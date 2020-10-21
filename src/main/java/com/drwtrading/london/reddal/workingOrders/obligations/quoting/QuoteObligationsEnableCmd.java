package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.application.User;

public class QuoteObligationsEnableCmd {

    public final User user;
    public final StackCommunity community;

    public QuoteObligationsEnableCmd(final User user, final StackCommunity community) {
        this.community = community;
        this.user = user;
    }
}
