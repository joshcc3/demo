package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.utils.application.User;

public class QuoteObligationsEnableCmd {

    public final User user;

    public QuoteObligationsEnableCmd(final User user) {

        this.user = user;
    }
}
