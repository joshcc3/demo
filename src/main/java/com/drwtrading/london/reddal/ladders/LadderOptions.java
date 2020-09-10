package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.reddal.fastui.html.CSSClass;

import java.util.Collection;

public class LadderOptions {

    public final Collection<CSSClass> orderTypesLeft;
    public final Collection<CSSClass> orderTypesRight;
    public final Collection<User> traders;
    final String basketUrl;

    public LadderOptions(final Collection<CSSClass> orderTypesLeft, final Collection<CSSClass> orderTypesRight,
            final Collection<User> traders, final String basketUrl) {

        this.orderTypesLeft = orderTypesLeft;
        this.orderTypesRight = orderTypesRight;
        this.traders = traders;
        this.basketUrl = basketUrl;
    }
}
