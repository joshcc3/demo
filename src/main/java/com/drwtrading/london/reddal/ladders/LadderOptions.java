package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.application.User;

import java.util.Collection;

public class LadderOptions {

    public final Collection<User> traders;
    final String basketUrl;

    public LadderOptions(final Collection<User> traders, final String basketUrl) {

        this.traders = traders;
        this.basketUrl = basketUrl;
    }
}
