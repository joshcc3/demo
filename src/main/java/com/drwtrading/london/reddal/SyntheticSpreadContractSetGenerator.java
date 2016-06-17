package com.drwtrading.london.reddal;

import com.drwtrading.london.reddal.symbols.SearchResult;
import org.jetlang.channels.Publisher;

public class SyntheticSpreadContractSetGenerator {

    public static final String SPREAD_PREFIX = "SPREAD:";
    final Publisher<SpreadContractSet> spreadContractSetPublisher;

    public SyntheticSpreadContractSetGenerator(final Publisher<SpreadContractSet> spreadContractSetPublisher) {
        this.spreadContractSetPublisher = spreadContractSetPublisher;
    }

    public void setSearchResult(final SearchResult searchResult) {
        handleSymbol(searchResult.symbol);
    }

    public void handleSymbol(final String symbol) {
        if (symbol.length() > SPREAD_PREFIX.length() && symbol.startsWith(SPREAD_PREFIX)) {
            final String substring = symbol.substring(SPREAD_PREFIX.length());
            final String[] split = substring.split("-");
            if (split.length == 2) {
                final String front = split[0];
                final String back = split[1];
                final SpreadContractSet contractSet = new SpreadContractSet(front, back, symbol);
                spreadContractSetPublisher.publish(contractSet);
            }
        }
    }

}
