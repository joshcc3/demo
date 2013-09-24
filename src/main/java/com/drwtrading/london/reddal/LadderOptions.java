package com.drwtrading.london.reddal;

import java.util.Collection;

public class LadderOptions {
    public final Collection<String> orderTypesLeft;
    public final Collection<String> orderTypesRight;
    public final Collection<String> traders;
    public final Environment.RemoteOrderServerResolver serverResolver;

    public LadderOptions(Collection<String> orderTypesLeft, Collection<String> orderTypesRight, Collection<String> traders, Environment.RemoteOrderServerResolver serverResolver) {
        this.orderTypesLeft = orderTypesLeft;
        this.orderTypesRight = orderTypesRight;
        this.traders = traders;
        this.serverResolver = serverResolver;
    }
}
