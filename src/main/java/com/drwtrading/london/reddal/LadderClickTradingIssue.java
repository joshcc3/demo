package com.drwtrading.london.reddal;

import com.drwtrading.london.util.Struct;

public class LadderClickTradingIssue extends Struct {
    public final String symbol;
    public final String issue;

    public LadderClickTradingIssue(String symbol, String issue) {
        this.symbol = symbol;
        this.issue = issue;
    }
}
