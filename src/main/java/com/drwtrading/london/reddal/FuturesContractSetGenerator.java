package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.google.common.collect.ImmutableSet;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;

public class FuturesContractSetGenerator {

    private final Set<String> excludedMarkets = ImmutableSet.of("FEXD");

    private final Map<String, NavigableMap<Long, SearchResult>> marketToOutrightsByExpiry;
    private final Map<String, HashMap<String, SearchResult>> spreadByExpires;
    private final Map<String, SpreadContractSet> setByFrontMonth;

    private final Publisher<SpreadContractSet> publisher;

    public FuturesContractSetGenerator(final Publisher<SpreadContractSet> publisher) {

        this.publisher = publisher;

        this.marketToOutrightsByExpiry = new HashMap<>();
        this.spreadByExpires = new HashMap<>();
        this.setByFrontMonth = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        switch (searchResult.instType) {
            case FUTURE: {

                final int contractLength = searchResult.symbol.length() - 2;
                final String contract = searchResult.symbol.substring(0, contractLength);
                final long expiryMilliSinceUTC = searchResult.expiry;
                MapUtils.getNavigableMap(marketToOutrightsByExpiry, contract).put(expiryMilliSinceUTC, searchResult);

                publishContractSet(contract);
                break;
            }
            case FUTURE_SPREAD: {

                final String[] legs = searchResult.symbol.split("-");

                final Map<String, SearchResult> spreadByBackMonth = MapUtils.getMappedMap(spreadByExpires, legs[0]);
                spreadByBackMonth.put(legs[1], searchResult);

                final int frontContractLength = legs[0].length() - 2;
                final String frontContract = legs[0].substring(0, frontContractLength);

                publishContractSet(frontContract);
                break;
            }
        }
    }

    private void publishContractSet(final String contract) {

        if (!excludedMarkets.contains(contract)) {
            final SpreadContractSet spreadContractSet = updateMarket(contract);
            if (null != spreadContractSet && !spreadContractSet.equals(setByFrontMonth.put(spreadContractSet.front, spreadContractSet))) {
                publisher.publish(spreadContractSet);
            }
        }
    }

    private SpreadContractSet updateMarket(final String contract) {

        final NavigableMap<Long, SearchResult> outrights = MapUtils.getNavigableMap(marketToOutrightsByExpiry, contract);

        if (outrights.isEmpty()) {

            return null;

        } else if (1 == outrights.size()) {

            final String symbol = outrights.firstEntry().getValue().symbol;
            return new SpreadContractSet(symbol, symbol, symbol);

        } else {

            final Iterator<SearchResult> expiries = outrights.values().iterator();

            final SearchResult firstExpiry = expiries.next();
            final SearchResult secondExpiry = expiries.next();

            final Map<String, SearchResult> spreadByBackMonth = MapUtils.getMappedMap(spreadByExpires, firstExpiry.symbol);
            final SearchResult spread = spreadByBackMonth.get(secondExpiry.symbol);

            if (null == spread) {
                return new SpreadContractSet(firstExpiry.symbol, secondExpiry.symbol, firstExpiry.symbol + '-' + secondExpiry.symbol);
            } else {
                return new SpreadContractSet(firstExpiry.symbol, secondExpiry.symbol, spread.symbol);
            }
        }
    }
}
