package com.drwtrading.london.reddal;

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
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.util.Struct;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.MapMaker;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class FuturesContractSetGenerator {

    private final Set<String> excludedMarkets = ImmutableSet.of("FEXD");

    private final Map<String, TreeMap<String, SearchResult>> marketToOutrightsByExpiry =
            new MapMaker().makeComputingMap(from -> new TreeMap<>());

    private final Map<String, HashMap<SpreadExpiries, SearchResult>> marketToSpreads =
            new MapMaker().makeComputingMap(from -> new HashMap<>());

    private final Map<String, SpreadContractSet> setByFrontMonth = new HashMap<>();

    private final Publisher<SpreadContractSet> publisher;

    public FuturesContractSetGenerator(final Publisher<SpreadContractSet> publisher) {
        this.publisher = publisher;
    }

    @Subscribe
    public void on(final InstrumentDefinitionEvent instrumentDefinitionEvent) {

        if (instrumentDefinitionEvent.getInstrumentStructure() instanceof FutureOutrightStructure) {

            final SearchResult searchResult = getSearchResult(instrumentDefinitionEvent);
            setSearchResult(searchResult);

        } else if (instrumentDefinitionEvent.getInstrumentStructure() instanceof FutureStrategyStructure) {

            final SearchResult searchResult = getSearchResult(instrumentDefinitionEvent);
            setSearchResult(searchResult);
        }
    }

    public static SearchResult getSearchResult(final InstrumentDefinitionEvent instDefEvent) {

        final String symbol = instDefEvent.getSymbol();
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

        final MDSource mdSource = MarketDataForSymbol.getSource(instDefEvent);

        final long expiry = getExpiry(instDefEvent);

        return new SearchResult(symbol, instID, instType, description, mdSource, terms, expiry, symbol);
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

    public void setSearchResult(final SearchResult searchResult) {

        switch (searchResult.instType) {
            case FUTURE: {
                final int contractLength = searchResult.symbol.length() - 2;
                final String contract = searchResult.symbol.substring(0, contractLength);
                final String expiry = reverseString(searchResult.symbol.substring(contractLength));
                marketToOutrightsByExpiry.get(contract).put(expiry, searchResult);
                publishContractSet(contract);
                break;
            }
            case FUTURE_SPREAD: {
                final String[] legs = searchResult.symbol.split("-");

                final int frontContractLength = legs[0].length() - 2;
                final String frontContract = legs[0].substring(0, frontContractLength);
                final String frontExpiry = reverseString(legs[0].substring(frontContractLength));

                final int backContractLength = legs[1].length() - 2;
                final String backExpiry = reverseString(legs[1].substring(backContractLength));

                final SpreadExpiries spreadExpiries = new SpreadExpiries(frontExpiry, backExpiry);
                marketToSpreads.get(frontContract).put(spreadExpiries, searchResult);
                publishContractSet(frontContract);
                break;
            }
        }
    }

    private static String reverseString(final String s) {

        final StringBuilder sb = new StringBuilder();
        for (int i = s.length() - 1; -1 < i; --i) {
            sb.append(s.charAt(i));
        }
        return sb.toString();
    }

    private void publishContractSet(final String market) {
        if (!excludedMarkets.contains(market)) {
            final SpreadContractSet spreadContractSet = updateMarket(market);
            if (spreadContractSet != null && !spreadContractSet.equals(setByFrontMonth.put(spreadContractSet.front, spreadContractSet))) {
                publisher.publish(spreadContractSet);
            }
        }
    }

    private SpreadContractSet updateMarket(final String market) {
        final TreeMap<String, SearchResult> outrights = marketToOutrightsByExpiry.get(market);
        if (outrights.isEmpty()) {
            return null;
        } else if (outrights.size() == 1) {
            return new SpreadContractSet(outrights.firstEntry().getValue().symbol, null, null);
        } else if (outrights.size() > 1) {
            final ArrayList<Map.Entry<String, SearchResult>> values = new ArrayList<>(outrights.entrySet());
            final Map.Entry<String, SearchResult> front = values.get(0);
            final Map.Entry<String, SearchResult> back = values.get(1);
            final HashMap<SpreadExpiries, SearchResult> spreads = marketToSpreads.get(market);
            final SearchResult spread = spreads.get(new SpreadExpiries(front.getKey(), back.getKey()));
            if (spread != null) {
                return new SpreadContractSet(front.getValue().symbol, back.getValue().symbol, spread.symbol);
            } else {
                return new SpreadContractSet(front.getValue().symbol, back.getValue().symbol,
                        front.getValue().symbol + '-' + back.getValue().symbol);
            }
        }
        return null;
    }

    public static class SpreadExpiries extends Struct {

        public final String frontExpiry;
        public final String backExpiry;

        public SpreadExpiries(final String frontExpiry, final String backExpiry) {
            this.frontExpiry = frontExpiry;
            this.backExpiry = backExpiry;
        }
    }

}
