package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
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
import com.google.common.collect.ImmutableSet;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
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

    @Subscribe
    public void on(final InstrumentDefinitionEvent instDef) {

        switch (instDef.getInstrumentStructure().typeEnum()) {
            case FUTURE_OUTRIGHT_STRUCTURE: {
                final SearchResult searchResult = getSearchResult(instDef);
                setSearchResult(searchResult);
                break;
            }
            case FUTURE_STRATEGY_STRUCTURE: {
                final SearchResult searchResult = getSearchResult(instDef);
                setSearchResult(searchResult);
                break;
            }
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
            if (spreadContractSet != null && !spreadContractSet.equals(setByFrontMonth.put(spreadContractSet.front, spreadContractSet))) {
                publisher.publish(spreadContractSet);
            }
        }
    }

    private SpreadContractSet updateMarket(final String market) {

        final NavigableMap<Long, SearchResult> outrights = MapUtils.getNavigableMap(marketToOutrightsByExpiry, market);

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
