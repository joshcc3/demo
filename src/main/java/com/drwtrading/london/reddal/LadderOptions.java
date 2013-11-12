package com.drwtrading.london.reddal;

import java.util.Collection;
import java.util.List;
import java.util.Set;

public class LadderOptions {
    public final Collection<String> orderTypesLeft;
    public final Collection<String> orderTypesRight;
    public final Collection<String> traders;
    public final List<String> shiftLaserLines;
    public final String tag;
    public final Environment.RemoteOrderServerResolver serverResolver;
    public final double randomReloadFraction;

    public LadderOptions(Collection<String> orderTypesLeft, Collection<String> orderTypesRight, Collection<String> traders, List<String> shiftLaserLines, String tag, Environment.RemoteOrderServerResolver serverResolver, double randomReloadFraction) {
        this.orderTypesLeft = orderTypesLeft;
        this.orderTypesRight = orderTypesRight;
        this.traders = traders;
        this.shiftLaserLines = shiftLaserLines;
        this.tag = tag;
        this.serverResolver = serverResolver;
        this.randomReloadFraction = randomReloadFraction;
    }
}
