package com.drwtrading.london;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.protocols.photon.marketdata.BatsInstrumentDefinition;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.ExchangeInstrumentDefinitionDetails;
import com.drwtrading.london.protocols.photon.marketdata.FutureLegStructure;
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
import java.util.Map;
import java.util.Set;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class IndexPresenter {

    SuffixTree<String> suffixTree = new SuffixTree<String>();
    WebSocketViews<View> views = WebSocketViews.create(View.class, this);
    Map<String, DisplaySymbol> symbolToDisplay = newFastMap();
    Map<String, SearchResult> searchResultBySymbol = newFastMap();

    private final int minTermLength = 3;
    private final int maxResults = 100;


    @Subscribe
    public void on(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        SearchResult searchResult = searchResultFromInstrumentDef(instrumentDefinitionEvent);
        searchResultBySymbol.put(searchResult.symbol, searchResult);
        for (String keyword : searchResult.keywords) {
            suffixTree.put(keyword, searchResult.symbol);
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
                    showDisplaySymbol(displaySymbol),
                    searchResult.link,
                    searchResult.description,
                    searchResult.keywords
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

        DisplaySymbol display = symbolToDisplay.get(symbol);
        if (display != null) {
            symbol = showDisplaySymbol(display);
            terms.add(display.displaySymbol);
        }

        terms.add(instrumentDefinitionEvent.getExchange());
        terms.add(company);

        terms.addAll(Arrays.asList(desc.split(("\\W"))));

        return new SearchResult(symbol, link, description, terms);
    }

    private String showDisplaySymbol(DisplaySymbol display) {
        return display.displaySymbol + " (" + display.marketDataSymbol + ")";
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

        displayResult(view, matching);

    }

    private void displayResult(View view, Set<String> matching) {

        ArrayList<String> symbols = new ArrayList<String>(matching);
        Collections.sort(symbols);

        ArrayList<SearchResult> results = new ArrayList<SearchResult>(Collections2.transform(symbols, new Function<String, SearchResult>() {
            @Override
            public SearchResult apply(String from) {
                return searchResultBySymbol.get(from);
            }
        }));

        if (results.size() < maxResults) {
            view.display(results, false);
        } else {
            view.display(results.subList(0, maxResults), true);
        }

    }


    public static interface View {
        void display(Collection<SearchResult> results, boolean tooMany);
    }


    public static class SearchResult extends Struct {
        public final String symbol;
        public final String link;
        public final String description;
        public final Collection<String> keywords;

        public SearchResult(String symbol, String link, String description, Collection<String> keywords) {
            this.symbol = symbol;
            this.link = link;
            this.description = description;
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


}
