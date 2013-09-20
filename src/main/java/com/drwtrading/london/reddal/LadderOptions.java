package com.drwtrading.london.reddal;

import java.util.Collection;

public class LadderOptions {
    public final Collection<String> orderTypesLeft;
    public final Collection<String> orderTypesRight;
    public final Collection<String> traders;
    public LadderOptions(Collection<String> orderTypesLeft, Collection<String> orderTypesRight, Collection<String> traders) {
        this.orderTypesLeft = orderTypesLeft;
        this.orderTypesRight = orderTypesRight;
        this.traders = traders;
    }
}
