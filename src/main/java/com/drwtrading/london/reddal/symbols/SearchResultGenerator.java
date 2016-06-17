package com.drwtrading.london.reddal.symbols;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.BasicTickTable;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.SingleBandTickTable;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.prices.PriceFormats;
import com.drwtrading.london.protocols.photon.marketdata.BatsInstrumentDefinition;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.CmeDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.CmeFractionalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.DecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.ExchangeInstrumentDefinitionDetails;
import com.drwtrading.london.protocols.photon.marketdata.FutureLegStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureStrategyStructure;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentStructure;
import com.drwtrading.london.protocols.photon.marketdata.LiffeThirtySecondsTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.NormalizedBandedDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.NormalizedDecimalTickStructure;
import com.drwtrading.london.protocols.photon.marketdata.TickBand;
import com.drwtrading.london.protocols.photon.marketdata.TickStructure;
import com.drwtrading.london.protocols.photon.marketdata.XetraInstrumentDefinition;
import com.drwtrading.london.reddal.data.MarketDataForSymbol;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

public class SearchResultGenerator {

    private final Publisher<SearchResult> searchResultPublisher;
    private final Map<String, SearchResult> searchResultBySymbol;
    private final Map<String, DisplaySymbol> symbolToDisplay;

    public SearchResultGenerator(final Publisher<SearchResult> searchResultPublisher) {

        this.searchResultPublisher = searchResultPublisher;
        this.searchResultBySymbol = new HashMap<>();
        this.symbolToDisplay = new HashMap<>();
    }

    @Subscribe
    public void setInstDefEvent(final InstrumentDefinitionEvent instDefEvent) {

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
                switch (((FutureStrategyStructure) instDefEvent.getInstrumentStructure()).getType()) {
                    case SPREAD: {
                        instType = InstType.FUTURE_SPREAD;
                        for (final FutureLegStructure futureLegStructure : ((FutureStrategyStructure) structure).getLegs()) {
                            desc = desc + futureLegStructure.getPosition() + 'x' + futureLegStructure.getSymbol();
                        }
                        break;
                    }
                    default: {
                        return;
                    }
                }
                break;
            }
            default: {
                instType = InstType.UNKNOWN;
            }
        }

        final ITickTable tickTable = getTickTable(instDefEvent);

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

        final String displaySymbol;
        if (symbolToDisplay.containsKey(symbol)) {
            displaySymbol = symbolToDisplay.get(symbol).displaySymbol;
            terms.add(displaySymbol);
        } else {
            displaySymbol = symbol;
        }

        final MDSource mdSource = MarketDataForSymbol.getSource(instDefEvent);

        final long expiry = getExpiry(instDefEvent);

        final SearchResult searchResult =
                new SearchResult(symbol, instID, instType, description, mdSource, terms, expiry, displaySymbol, tickTable);
        setSearchResult(searchResult);
    }

    @Subscribe
    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {

        symbolToDisplay.put(displaySymbol.marketDataSymbol, displaySymbol);

        final SearchResult searchResult = searchResultBySymbol.get(displaySymbol.marketDataSymbol);
        if (null != searchResult && !searchResult.displaySymbol.equals(displaySymbol.displaySymbol)) {

            searchResult.keywords.add(displaySymbol.displaySymbol);
            final SearchResult newResult =
                    new SearchResult(searchResult.symbol, searchResult.instID, searchResult.instType, searchResult.description,
                            searchResult.mdSource, searchResult.keywords, searchResult.expiry, displaySymbol.displaySymbol,
                            searchResult.tickTable);
            setSearchResult(newResult);
        }
    }

    private void setSearchResult(final SearchResult searchResult) {

        searchResultBySymbol.put(searchResult.symbol, searchResult);
        searchResultPublisher.publish(searchResult);
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

    private static ITickTable getTickTable(final InstrumentDefinitionEvent def) {
        return def.getPriceStructure().getTickStructure().accept(new TickStructure.Visitor<ITickTable>() {
            @Override
            public ITickTable visitNormalizedDecimalTickStructure(final NormalizedDecimalTickStructure msg) {
                return new SingleBandTickTable(def.getPriceStructure().getTickIncrement());
            }

            @Override
            public ITickTable visitNormalizedBandedDecimalTickStructure(final NormalizedBandedDecimalTickStructure msg) {
                final NavigableMap<Long, Long> tickLevels = new TreeMap<>();
                for (final TickBand band : msg.getBands()) {
                    tickLevels.put(band.getMinPrice(), band.getTickSize());
                }
                return new BasicTickTable(tickLevels);
            }

            @Override
            public ITickTable visitDecimalTickStructure(final DecimalTickStructure msg) {
                if (msg.getPointPosition() == PriceFormats.NORMAL_POINT_POSITION) {
                    return new SingleBandTickTable(def.getPriceStructure().getTickIncrement());
                }
                throw new IllegalArgumentException("Unsupported tick structure: " + msg);
            }

            @Override
            public ITickTable visitCmeDecimalTickStructure(final CmeDecimalTickStructure msg) {
                throw new IllegalArgumentException("Unsupported tick structure: " + msg);
            }

            @Override
            public ITickTable visitCmeFractionalTickStructure(final CmeFractionalTickStructure msg) {
                throw new IllegalArgumentException("Unsupported tick structure: " + msg);
            }

            @Override
            public ITickTable visitLiffeThirtySecondsTickStructure(final LiffeThirtySecondsTickStructure msg) {
                throw new IllegalArgumentException("Unsupported tick structure: " + msg);
            }
        });
    }

}
