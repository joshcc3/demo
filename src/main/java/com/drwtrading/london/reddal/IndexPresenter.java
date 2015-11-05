package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.protocols.photon.marketdata.BatsInstrumentDefinition;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.ExchangeInstrumentDefinitionDetails;
import com.drwtrading.london.protocols.photon.marketdata.FutureLegStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureStrategyStructure;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentStructure;
import com.drwtrading.london.protocols.photon.marketdata.XetraInstrumentDefinition;
import com.drwtrading.london.reddal.data.DisplaySymbol;
import com.drwtrading.london.util.Struct;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import it.unimi.dsi.fastutil.objects.Object2ObjectAVLTreeMap;
import it.unimi.dsi.fastutil.objects.Object2ObjectMap;
import it.unimi.dsi.fastutil.objects.ObjectArraySet;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class IndexPresenter {

    SuffixTree<String> suffixTree = new SuffixTree<String>();
    WebSocketViews<View> views = WebSocketViews.create(View.class, this);
    Map<String, DisplaySymbol> symbolToDisplay = newFastMap();
    Map<String, SearchResult> searchResultBySymbol = newFastMap();
    Map<String, InstrumentDefinitionEvent> instrumentDefinitionEventMap = newFastMap();

    private final int minTermLength = 2;
    private final int maxResults = 100;

    @Subscribe
    public void on(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        instrumentDefinitionEventMap.put(instrumentDefinitionEvent.getSymbol(), instrumentDefinitionEvent);
        SearchResult searchResult = searchResultFromInstrumentDef(instrumentDefinitionEvent);
        if (searchResultBySymbol.put(searchResult.symbol, searchResult) == null) {
            for (String keyword : searchResult.keywords) {
                suffixTree.put(keyword, searchResult.symbol);
            }
        }
    }

    @Subscribe
    public void on(DisplaySymbol displaySymbol) {
        symbolToDisplay.put(displaySymbol.marketDataSymbol, displaySymbol);
        suffixTree.put(displaySymbol.displaySymbol, displaySymbol.marketDataSymbol);
        if (searchResultBySymbol.containsKey(displaySymbol.marketDataSymbol)) {
            SearchResult searchResult = searchResultBySymbol.get(displaySymbol.marketDataSymbol);
            searchResult.keywords.add(displaySymbol.displaySymbol);
            searchResultBySymbol.put(searchResult.symbol, new SearchResult(
                    searchResult.symbol,
                    displaySymbol.displaySymbol,
                    searchResult.link,
                    searchResult.description,
                    searchResult.exchange, searchResult.keywords
            ));
        }

    }

    private SearchResult searchResultFromInstrumentDef(InstrumentDefinitionEvent instrumentDefinitionEvent) {

        String symbol = instrumentDefinitionEvent.getSymbol();
        String link = "/ladder#" + symbol;

        String company = "";
        ExchangeInstrumentDefinitionDetails details = instrumentDefinitionEvent.getExchangeInstrumentDefinitionDetails();
        if (details instanceof BatsInstrumentDefinition) {
            company = ((BatsInstrumentDefinition) details).getCompanyName();
        } else if (details instanceof XetraInstrumentDefinition) {
            company = ((XetraInstrumentDefinition) details).getLongName();
        }

        String desc = "";
        InstrumentStructure structure = instrumentDefinitionEvent.getInstrumentStructure();
        if (structure instanceof CashOutrightStructure) {
            desc = ((CashOutrightStructure) structure).getIsin() + "." + instrumentDefinitionEvent.getPriceStructure().getCurrency().toString() + "." + ((CashOutrightStructure) structure).getMic();
        } else if (structure instanceof FutureStrategyStructure) {
            for (FutureLegStructure futureLegStructure : ((FutureStrategyStructure) structure).getLegs()) {
                desc = desc + futureLegStructure.getPosition() + "x" + futureLegStructure.getSymbol();
            }
        }

        String description =
                instrumentDefinitionEvent.getPriceStructure().getCurrency().toString() + " "
                        + instrumentDefinitionEvent.getExchange() + " "
                        + company + " " + desc;

        ArrayList<String> terms = new ArrayList<String>();

        terms.add(symbol);

        if (instrumentDefinitionEvent.getInstrumentStructure().typeEnum() == InstrumentStructure.Type.FOREX_PAIR_STRUCTURE) {
            terms.add(symbol.replace("/", ""));
        }

        DisplaySymbol display = symbolToDisplay.get(symbol);
        if (display != null) {
            symbol = showDisplaySymbol(display);
            terms.add(display.displaySymbol);
        }

        terms.add(instrumentDefinitionEvent.getExchange());
        terms.add(company);

        terms.addAll(Arrays.asList(desc.split(("\\W"))));

        return new SearchResult(symbol, display != null ? display.displaySymbol : null, link, description, instrumentDefinitionEvent.getExchange(), terms);
    }

    private String showDisplaySymbol(DisplaySymbol display) {
        return display.displaySymbol;
    }


    @Subscribe
    public void on(WebSocketConnected connected) {
        views.register(connected);
    }

    @Subscribe
    public void on(WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    @Subscribe
    public void on(WebSocketInboundData inboundData) {
        views.invoke(inboundData);
    }

    @FromWebSocketView
    public void search(String searchTerms, WebSocketInboundData data) {

        View view = views.get(data.getOutboundChannel());

        Set<String> matching = new ObjectArraySet<String>();
        if (searchTerms.length() > minTermLength) {
            matching.addAll(suffixTree.search(searchTerms));
        }

        for (String term : searchTerms.split("\\W")) {
            if (term.length() < minTermLength) {
                continue;
            }
            Set<String> result = suffixTree.search(term);
            if (matching.isEmpty()) {
                matching = Sets.union(matching, result);
            } else {
                matching = Sets.intersection(matching, result);
            }
        }

        displayResult(view, matching, searchTerms);

    }

    private void displayResult(View view, Set<String> matching, final String searchTerms) {
        ArrayList<String> symbols = new ArrayList<>(matching);
        Collections.sort(symbols, (o1, o2) -> {
            int distance = LevenshteinDistance.computeEditDistance(searchTerms, o1) - LevenshteinDistance.computeEditDistance(searchTerms, o2);
            if (distance == 0) {
                long exp1 = getExpiry(instrumentDefinitionEventMap.get(o1));
                long exp2 = getExpiry(instrumentDefinitionEventMap.get(o2));
                if(exp1 != 0 && exp2 != 0 && exp1 - exp2 != 0) {
                    return (exp1 - exp2) < 0 ? -1 : 1;
                }
                return o1.compareToIgnoreCase(o2);
            }
            return distance;
        });

        if (symbols.remove(searchTerms.toUpperCase())) {
            symbols.add(0, searchTerms.toUpperCase());
        } else if (symbols.remove(("SF:" + searchTerms.toUpperCase()))) {
            symbols.add(0, "SF:" + searchTerms.toUpperCase());
        }

        List<InstrumentDefinitionEvent> defs = symbols.stream()
                .filter(s -> s.replaceAll("SF:","").split(" ")[0].toUpperCase().startsWith(searchTerms.trim().toUpperCase()))
                .map(s -> instrumentDefinitionEventMap.get(s))
                .filter(d -> d != null)
                .filter(d -> d.getExchangeInstrumentDefinitionDetails().typeEnum() != ExchangeInstrumentDefinitionDetails.Type.BATS_INSTRUMENT_DEFINITION)
                .collect(Collectors.toList());

        if (!defs.isEmpty()) {
            symbols.add(0, defs.get(0).getSymbol());
        }

        List<SearchResult> results = symbols.stream().distinct()
                .map(from -> searchResultBySymbol.get(from))
                .collect(Collectors.toList());

        if (results.size() < maxResults) {
            view.display(results, false);
        } else {
            view.display(results.subList(0, maxResults), true);
        }

    }


    public static interface View {
        void display(Collection<SearchResult> results, boolean tooMany);
    }

    private long getExpiry(InstrumentDefinitionEvent e1) {
        long exp1 = 0;
        if(e1 == null) {
            return 0;
        }
        if(e1.getInstrumentStructure() instanceof FutureOutrightStructure) {
            exp1 = ((FutureOutrightStructure) e1.getInstrumentStructure()).getExpiry().getTimestamp();
        } else  if (e1.getInstrumentStructure() instanceof FutureStrategyStructure) {
            exp1 = ((FutureStrategyStructure) e1.getInstrumentStructure()).getLegs().get(0).getExpiry().getTimestamp();
        } else {
            exp1 = 0;
        }
        return exp1;
    }


    public static class SearchResult extends Struct {
        public final String symbol;
        public final String displaySymbol;
        public final String link;
        public final String description;
        public final String exchange;
        public final Collection<String> keywords;

        public SearchResult(String symbol, String displaySymbol, String link, String description, String exchange, Collection<String> keywords) {
            this.symbol = symbol;
            this.displaySymbol = displaySymbol;
            this.link = link;
            this.description = description;
            this.exchange = exchange;
            this.keywords = keywords;
        }
    }


    public static class SuffixTree<T> {

        public final Object2ObjectAVLTreeMap<String, Set<T>> suffixTree = new Object2ObjectAVLTreeMap<String, Set<T>>();

        public void put(String key, T value) {
            key = normalize(key);
            for (int i = 0; i < key.length(); i++) {
                insert(key.substring(i), value);
            }
        }

        private String normalize(String key) {
            return key.toUpperCase().replace("\\W", "");
        }

        public Set<T> search(String search) {
            search = normalize(search);
            ObjectArraySet<T> results = new ObjectArraySet<T>();
            for (Object2ObjectMap.Entry<String, Set<T>> entry : suffixTree.tailMap(search).object2ObjectEntrySet()) {
                if (!entry.getKey().startsWith(search)) {
                    break;
                }
                results.addAll(entry.getValue());
            }
            return results;
        }

        private void insert(String substring, T value) {
            if (!suffixTree.containsKey(substring)) {
                suffixTree.put(substring, new ObjectArraySet<T>());
            }
            suffixTree.get(substring).add(value);
        }

    }

    public static class LevenshteinDistance {

        public static double similarity(String s1, String s2) {
            if (s1.length() < s2.length()) { // s1 should always be bigger
                String swap = s1;
                s1 = s2;
                s2 = swap;
            }
            int bigLen = s1.length();
            if (bigLen == 0) {
                return 1.0; /* both strings are zero length */
            }
            return (bigLen - computeEditDistance(s1, s2)) / (double) bigLen;
        }

        public static int computeEditDistance(String s1, String s2) {
            if (s1.length() < s2.length()) { // s1 should always be bigger
                String swap = s1;
                s1 = s2;
                s2 = swap;
            }

            s1 = s1.toLowerCase();
            s2 = s2.toLowerCase();

            int[] costs = new int[s2.length() + 1];
            for (int i = 0; i <= s1.length(); i++) {
                int lastValue = i;
                for (int j = 0; j <= s2.length(); j++) {
                    if (i == 0)
                        costs[j] = j;
                    else {
                        if (j > 0) {
                            int newValue = costs[j - 1];
                            if (s1.charAt(i - 1) != s2.charAt(j - 1))
                                newValue = Math.min(Math.min(newValue, lastValue),
                                        costs[j]) + 1;
                            costs[j - 1] = lastValue;
                            lastValue = newValue;
                        }
                    }
                }
                if (i > 0)
                    costs[s2.length()] = lastValue;
            }
            return costs[s2.length()];
        }


    }
}
