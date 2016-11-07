package com.drwtrading.london.reddal;

import com.drwtrading.london.util.Struct;

public class ReplaceCommand extends Struct {

    public final String user;
    public final String from;
    public final String to;

    public ReplaceCommand(final String user, final String from, final String to) {
        this.user = user;
        this.from = from;
        this.to = to;
    }
}
