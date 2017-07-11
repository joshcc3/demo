package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.reddal.Environment;
import com.drwtrading.london.reddal.fastui.html.CSSClass;

import java.util.Collection;

public class LadderOptions {

    public final Collection<CSSClass> orderTypesLeft;
    public final Collection<CSSClass> orderTypesRight;
    public final Collection<String> traders;
    public final String theoLaserLineID;
    public final Environment.RemoteOrderServerResolver serverResolver;
    public final double randomReloadFraction;
    public final String basketUrl;

    public LadderOptions(final Collection<CSSClass> orderTypesLeft, final Collection<CSSClass> orderTypesRight,
            final Collection<String> traders, final String theoLaserLineID, final Environment.RemoteOrderServerResolver serverResolver,
            final double randomReloadFraction, final String basketUrl) {

        this.orderTypesLeft = orderTypesLeft;
        this.orderTypesRight = orderTypesRight;
        this.traders = traders;
        this.theoLaserLineID = theoLaserLineID;
        this.serverResolver = serverResolver;
        this.randomReloadFraction = randomReloadFraction;
        this.basketUrl = basketUrl;
    }
}
