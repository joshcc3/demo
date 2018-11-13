package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.reddal.fastui.html.CSSClass;

import java.util.Collection;

public class LadderOptions {

    public final Collection<CSSClass> orderTypesLeft;
    public final Collection<CSSClass> orderTypesRight;
    public final Collection<String> traders;
    public final double randomReloadFraction;
    public final String basketUrl;

    public LadderOptions(final Collection<CSSClass> orderTypesLeft, final Collection<CSSClass> orderTypesRight,
            final Collection<String> traders, final double randomReloadFraction, final String basketUrl) {

        this.orderTypesLeft = orderTypesLeft;
        this.orderTypesRight = orderTypesRight;
        this.traders = traders;
        this.randomReloadFraction = randomReloadFraction;
        this.basketUrl = basketUrl;
    }
}
