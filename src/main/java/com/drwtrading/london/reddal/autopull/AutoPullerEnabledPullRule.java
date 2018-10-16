package com.drwtrading.london.reddal.autopull;

import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class AutoPullerEnabledPullRule {

    PullRule pullRule;
    private String enabledByUser;

    AutoPullerEnabledPullRule(final PullRule pullRule) {
        this.pullRule = pullRule;
    }

    boolean isEnabled() {
        return null != enabledByUser;
    }

    void disable() {
        this.enabledByUser = null;
    }

    PullRule getPullRule() {
        return pullRule;
    }

    List<RemoteOrderCommandToServer> ordersToPull(final Set<SourcedWorkingOrder> workingOrders, final IBook<?> book) {
        if (isEnabled()) {
            return this.pullRule.ordersToPull(enabledByUser, workingOrders, book);
        } else {
            return Collections.emptyList();
        }
    }

    void enable(final String user) {
        this.enabledByUser = user;
    }

    void setPullRule(final PullRule pullRule) {
        this.pullRule = pullRule;
    }

    String getEnabledByUser() {
        return enabledByUser;
    }
}
