package com.drwtrading.london.reddal.autopull.autopuller.onMD;

import com.drwtrading.london.reddal.autopull.autopuller.rules.PullRule;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.orderManagement.RemoteOrderCommandToServer;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;

import java.util.Collections;
import java.util.List;
import java.util.Set;

class AutoPullerPullRule {

    public final PullRule pullRule;
    public final MDForSymbol md;

    private String enabledByUser;

    AutoPullerPullRule(final PullRule pullRule, final MDForSymbol md) {
        this.pullRule = pullRule;
        this.md = md;
    }

    void enable(final String user) {
        this.enabledByUser = user;
    }

    void disable() {
        this.enabledByUser = null;
    }

    String getAssociatedUser() {
        return enabledByUser;
    }

    boolean isEnabled() {
        return null != enabledByUser;
    }

    List<RemoteOrderCommandToServer> getOrdersToPull(final Set<SourcedWorkingOrder> workingOrders) {

        if (null != md.getBook() && null != workingOrders && !workingOrders.isEmpty()) {
            return this.pullRule.getPullCmds(enabledByUser, workingOrders, md.getBook());
        } else {
            return Collections.emptyList();
        }
    }
}
