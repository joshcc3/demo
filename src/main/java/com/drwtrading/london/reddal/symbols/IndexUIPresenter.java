package com.drwtrading.london.reddal.symbols;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.protocols.photon.marketdata.BatsInstrumentDefinition;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.ExchangeInstrumentDefinitionDetails;
import com.drwtrading.london.protocols.photon.marketdata.FutureLegStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureStrategyStructure;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentStructure;
import com.drwtrading.london.protocols.photon.marketdata.XetraInstrumentDefinition;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import com.drwtrading.london.reddal.util.FastUtilCollections;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class IndexUIPresenter {

    private static final int MIN_TERM_LENGTH = 2;
    private static final int MAX_RESULTS = 100;

    private final SuffixTree<String> suffixTree;
    private final WebSocketViews<IndexUIView> views;
    private final Map<String, DisplaySymbol> symbolToDisplay;
    private final Map<String, SearchResult> searchResultBySymbol;
    private final Map<String, InstrumentDefinitionEvent> instDefsMap;

    public IndexUIPresenter() {

        this.suffixTree = new SuffixTree<>();
        this.views = WebSocketViews.create(IndexUIView.class, this);
        this.symbolToDisplay = FastUtilCollections.newFastMap();
        this.searchResultBySymbol = FastUtilCollections.newFastMap();
        this.instDefsMap = FastUtilCollections.newFastMap();
    }

    @Subscribe
    public void on(final InstrumentDefinitionEvent instDefEvent) {

        instDefsMap.put(instDefEvent.getSymbol(), instDefEvent);

        String symbol = instDefEvent.getSymbol();
        String company = "";

        final InstrumentID instID = MarketDataForSymbol.getInstrumentID(instDefEvent);

        final InstType instType;
        String desc = "";
        final InstrumentStructure structure = instDefEvent.getInstrumentStructure();
        switch (structure.typeEnum()) {
            case CASH_OUTRIGHT_STRUCTURE: {
                instType = InstType.EQUITY;
                desc = ((CashOutrightStructure) structure).getIsin() + '.' +
                        instDefEvent.getPriceStructure().getCurrency().toString() + '.' +
                        ((CashOutrightStructure) structure).getMic();

                final ExchangeInstrumentDefinitionDetails details = instDefEvent.getExchangeInstrumentDefinitionDetails();
                switch (details.typeEnum()) {
                    case BATS_INSTRUMENT_DEFINITION: {
                        company = ((BatsInstrumentDefinition) details).getCompanyName();
                        break;
                    }
                    case XETRA_INSTRUMENT_DEFINITION: {
                        company = ((XetraInstrumentDefinition) details).getLongName();
                        break;
                    }
                }
                break;
            }
            case FOREX_PAIR_STRUCTURE: {
                instType = InstType.FX;
                break;
            }
            case FUTURE_OUTRIGHT_STRUCTURE: {
                instType = InstType.FUTURE;
                break;
            }
            case FUTURE_STRATEGY_STRUCTURE: {
                instType = InstType.FUTURE_SPREAD;
                for (final FutureLegStructure futureLegStructure : ((FutureStrategyStructure) structure).getLegs()) {
                    desc = desc + futureLegStructure.getPosition() + 'x' + futureLegStructure.getSymbol();
                }
                break;
            }
            default: {
                instType = InstType.UNKNOWN;
            }
        }

        final String description = instDefEvent.getPriceStructure().getCurrency().toString() + ' ' + instDefEvent.getExchange() +
                ' ' + company + ' ' + desc;

        final ArrayList<String> terms = new ArrayList<>();

        terms.add(symbol);

        if (instDefEvent.getInstrumentStructure().typeEnum() == InstrumentStructure.Type.FOREX_PAIR_STRUCTURE) {
            terms.add(symbol.replace("/", ""));
        }

        terms.add(instDefEvent.getExchange());
        terms.add(company);

        terms.addAll(Arrays.asList(desc.split(("\\W"))));

        String displaySymbol;
        if (symbolToDisplay.containsKey(symbol)) {
            displaySymbol = symbolToDisplay.get(symbol).displaySymbol;
            terms.add(displaySymbol);
        } else {
            displaySymbol = symbol;
        }

        final MDSource mdSource = MarketDataForSymbol.getSource(instDefEvent);

        final long expiry = getExpiry(instDefEvent);

        final SearchResult searchResult = new SearchResult(symbol, instID, instType, description, mdSource, terms, expiry, displaySymbol);
        setSearchResult(searchResult);
    }

    public void addSearchResult(final SearchResult searchResult) {

        final String symbol = searchResult.symbol;
        final DisplaySymbol displaySymbol = symbolToDisplay.get(symbol);

        if (null == displaySymbol) {
            setSearchResult(searchResult);
        } else {

            final Collection<String> keywords = new ArrayList<>(searchResult.keywords);
            keywords.add(displaySymbol.displaySymbol);
            final SearchResult newResult =
                    new SearchResult(searchResult.symbol, searchResult.instID, searchResult.instType,
                            searchResult.description, searchResult.mdSource, keywords, searchResult.expiry, displaySymbol.displaySymbol);
            setSearchResult(newResult);
        }
    }

    private void setSearchResult(final SearchResult searchResult) {
        searchResultBySymbol.put(searchResult.symbol, searchResult);
        for (final String keyword : searchResult.keywords) {
            suffixTree.put(keyword, searchResult.symbol);
        }
    }

    @Subscribe
    public void on(final DisplaySymbol displaySymbol) {
        symbolToDisplay.put(displaySymbol.marketDataSymbol, displaySymbol);
        if (searchResultBySymbol.containsKey(displaySymbol.marketDataSymbol)) {
            final SearchResult searchResult = searchResultBySymbol.get(displaySymbol.marketDataSymbol);
            if (!searchResult.displaySymbol.equals(displaySymbol.displaySymbol)) {
                searchResult.keywords.add(displaySymbol.displaySymbol);
                final SearchResult newResult =
                        new SearchResult(searchResult.symbol, searchResult.instID, searchResult.instType,
                                searchResult.description, searchResult.mdSource, searchResult.keywords, searchResult.expiry, displaySymbol.displaySymbol);
                searchResultBySymbol.put(searchResult.symbol, newResult);
                setSearchResult(searchResult);
            }
        }
    }

    private static long getExpiry(final InstrumentDefinitionEvent e1) {

        if (e1 == null) {
            return 0;
        } else if (e1.getInstrumentStructure() instanceof FutureOutrightStructure) {
            return ((FutureOutrightStructure) e1.getInstrumentStructure()).getExpiry().getTimestamp();
        } else if (e1.getInstrumentStructure() instanceof FutureStrategyStructure) {
            return ((FutureStrategyStructure) e1.getInstrumentStructure()).getLegs().get(0).getExpiry().getTimestamp();
        } else {
            return 0;
        }
    }

    @Subscribe
    public void on(final WebSocketConnected connected) {
        views.register(connected);
    }

    @Subscribe
    public void on(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    @Subscribe
    public void on(final WebSocketInboundData inboundData) {
        views.invoke(inboundData);
    }

    @FromWebSocketView
    public void search(final String searchTerms, final WebSocketInboundData data) {

        final IndexUIView view = views.get(data.getOutboundChannel());

        Set<String> matching = new ObjectArraySet<>();
        if (searchTerms.length() > MIN_TERM_LENGTH) {
            matching.addAll(suffixTree.search(searchTerms));
        }

        for (final String term : searchTerms.split("\\W")) {
            if (term.length() < MIN_TERM_LENGTH) {
                continue;
            }
            final Set<String> result = suffixTree.search(term);
            if (matching.isEmpty()) {
                matching = Sets.union(matching, result);
            } else {
                matching = Sets.intersection(matching, result);
            }
        }

        displayResult(view, matching, searchTerms);
    }

    private void displayResult(final IndexUIView view, final Set<String> matching, final String searchTerms) {

        final ArrayList<String> symbols = new ArrayList<>(matching);
        Collections.sort(symbols, (o1, o2) -> {
            final int distance = computeLevenshteinEditDistance(searchTerms, o1) - computeLevenshteinEditDistance(searchTerms, o2);
            if (0 != distance) {
                return distance;
            } else {
                final long exp1 = searchResultBySymbol.get(o1).expiry;
                final long exp2 = searchResultBySymbol.get(o2).expiry;
                if (0 != exp1 && 0 != exp2 && 0 != exp1 - exp2) {
                    return (exp1 - exp2) < 0 ? -1 : 1;
                } else {
                    return o1.compareToIgnoreCase(o2);
                }
            }
        });

        if (symbols.remove(searchTerms.toUpperCase())) {
            symbols.add(0, searchTerms.toUpperCase());
        }

        final List<InstrumentDefinitionEvent> defs =
                symbols.stream().filter(s -> s.split(" ")[0].toUpperCase().startsWith(searchTerms.trim().toUpperCase())).map(
                        instDefsMap::get).filter(d -> d != null).filter(d -> d.getExchangeInstrumentDefinitionDetails().typeEnum() !=
                        ExchangeInstrumentDefinitionDetails.Type.BATS_INSTRUMENT_DEFINITION).collect(Collectors.toList());

        if (!defs.isEmpty()) {
            symbols.add(0, defs.get(0).getSymbol());
        }

        final List<SearchResult> results = symbols.stream().distinct().map(searchResultBySymbol::get).collect(Collectors.toList());

        if (results.size() < MAX_RESULTS) {
            view.display(results, false);
        } else {
            view.display(results.subList(0, MAX_RESULTS), true);
        }

    }

    private static int computeLevenshteinEditDistance(String s1, String s2) {

        s1 = s1.toLowerCase();
        s2 = s2.toLowerCase();

        if (s1.length() < s2.length()) {
            final String swap = s1;
            s1 = s2;
            s2 = swap;
        }

        final int[] costs = new int[s2.length() + 1];
        for (int i = 0; i <= s1.length(); i++) {
            int lastValue = i;
            for (int j = 0; j <= s2.length(); j++) {
                if (i == 0) {
                    costs[j] = j;
                } else {
                    if (j > 0) {
                        int newValue = costs[j - 1];
                        if (s1.charAt(i - 1) != s2.charAt(j - 1)) {
                            newValue = Math.min(Math.min(newValue, lastValue), costs[j]) + 1;
                        }
                        costs[j - 1] = lastValue;
                        lastValue = newValue;
                    }
                }
            }
            if (0 < i) {
                costs[s2.length()] = lastValue;
            }
        }
        return costs[s2.length()];
    }
}