package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.fastui.html.CSSClass;
import com.drwtrading.london.reddal.Environment;

import java.util.Collection;

public class LadderOptions {

    public final Collection<CSSClass> orderTypesLeft;
    public final Collection<CSSClass> orderTypesRight;
    public final Collection<String> traders;
    public final String theoLaserLine;
    public final Environment.RemoteOrderServerResolver serverResolver;
    public final double randomReloadFraction;
    public final String basketUrl;

    public LadderOptions(final Collection<CSSClass> orderTypesLeft, final Collection<CSSClass> orderTypesRight,
            final Collection<String> traders, final String theoLaserLine, final Environment.RemoteOrderServerResolver serverResolver,
            final double randomReloadFraction, final String basketUrl) {

        this.orderTypesLeft = orderTypesLeft;
        this.orderTypesRight = orderTypesRight;
        this.traders = traders;
        this.theoLaserLine = theoLaserLine;
        this.serverResolver = serverResolver;
        this.randomReloadFraction = randomReloadFraction;
        this.basketUrl = basketUrl;
    }
}