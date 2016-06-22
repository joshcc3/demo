package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.util.Struct;

public class LadderClickTradingIssue extends Struct {
    public final String symbol;
    public final String issue;

    public LadderClickTradingIssue(final String symbol, final String issue) {
        this.symbol = symbol;
        this.issue = issue;
    }
}
