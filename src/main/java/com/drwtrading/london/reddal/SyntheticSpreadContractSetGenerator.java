package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import org.jetlang.channels.Publisher;

public class SyntheticSpreadContractSetGenerator {

    public static final String SPREAD_PREFIX = "SPREAD:";
    final Publisher<SpreadContractSet> spreadContractSetPublisher;

    public SyntheticSpreadContractSetGenerator(Publisher<SpreadContractSet> spreadContractSetPublisher) {
        this.spreadContractSetPublisher = spreadContractSetPublisher;
    }

    @Subscribe
    public void on(InstrumentDefinitionEvent def) {
        String symbol = def.getSymbol();
        if (symbol.length() > SPREAD_PREFIX.length() && symbol.startsWith(SPREAD_PREFIX)) {
            String substring = symbol.substring(SPREAD_PREFIX.length());
            String[] split = substring.split("-");
            if (split.length == 2) {
                String front = split[0];
                String back = split[1];
                SpreadContractSet contractSet = new SpreadContractSet(front, back, symbol);
            }
        }
    }

}
