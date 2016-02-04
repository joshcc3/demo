package com.drwtrading.london.reddal;

import java.util.Collection;

public class LadderOptions {

    public final Collection<String> orderTypesLeft;
    public final Collection<String> orderTypesRight;
    public final Collection<String> traders;
    public final String theoLaserLine;
    public final Environment.RemoteOrderServerResolver serverResolver;
    public final double randomReloadFraction;
    public final String basketUrl;

    public LadderOptions(Collection<String> orderTypesLeft, Collection<String> orderTypesRight, Collection<String> traders,
                         String theoLaserLine, Environment.RemoteOrderServerResolver serverResolver, double randomReloadFraction, String basketUrl) {
        this.orderTypesLeft = orderTypesLeft;
        this.orderTypesRight = orderTypesRight;
        this.traders = traders;
        this.theoLaserLine = theoLaserLine;
        this.serverResolver = serverResolver;
        this.randomReloadFraction = randomReloadFraction;
        this.basketUrl = basketUrl;
    }
}
