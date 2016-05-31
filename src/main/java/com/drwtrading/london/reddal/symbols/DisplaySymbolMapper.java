package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DisplaySymbolMapper {

    private final Multimap<String, String> mdSymbolsByIsin = HashMultimap.create();
    private final Map<String, String> bbgByIsin = new HashMap<>();
    private final Set<DisplaySymbol> displaySymbols = new HashSet<>();

    private final Publisher<DisplaySymbol> displaySymbolPublisher;

    public DisplaySymbolMapper(final Publisher<DisplaySymbol> displaySymbolPublisher) {
        this.displaySymbolPublisher = displaySymbolPublisher;
    }

    public void setInstDefEvent(final InstrumentDefinitionEvent instrumentDefinitionEvent) {

        if (instrumentDefinitionEvent.getInstrumentStructure() instanceof CashOutrightStructure) {

            final String isin = ((CashOutrightStructure) instrumentDefinitionEvent.getInstrumentStructure()).getIsin();
            final String symbol = instrumentDefinitionEvent.getSymbol();
            mdSymbolsByIsin.put(isin, symbol);

            final String bbgCode = bbgByIsin.get(isin);
            if (null != bbgCode) {
                makeDisplaySymbol(symbol, bbgCode);
            }
        } else if (instrumentDefinitionEvent.getInstrumentStructure() instanceof FutureOutrightStructure) {

            final String market = instrumentDefinitionEvent.getMarket();
            final SimpleDateFormat sdf = new SimpleDateFormat("MMM yy");

            final FutureOutrightStructure futureDetails = ((FutureOutrightStructure) instrumentDefinitionEvent.getInstrumentStructure());
            final long expiryMillis = futureDetails.getExpiry().getTimestamp();
            final Date expiryTime = new Date(expiryMillis);
            final String expiry = sdf.format(expiryTime);
            final DisplaySymbol displaySymbol = new DisplaySymbol(instrumentDefinitionEvent.getSymbol(), market + ' ' + expiry);
            publishIfNew(displaySymbol);
        }
    }

    public void setSearchResult(SearchResult searchResult) {
        if(searchResult.instType == InstType.EQUITY || searchResult.instType == InstType.DR ||
                searchResult.instType == InstType.ETF) {
            mdSymbolsByIsin.put(searchResult.instID.isin, searchResult.symbol);
            final String bbgCode = bbgByIsin.get(searchResult.instID.isin);
            if (null != bbgCode) {
                makeDisplaySymbol(searchResult.symbol, bbgCode);
            }
        }
    }


    public void setInstDef(final InstrumentDef instDef) {

        if (instDef.isPrimary) {
            bbgByIsin.put(instDef.instID.isin, instDef.bbgCode);
            for (final String marketDataSymbol : mdSymbolsByIsin.get(instDef.instID.isin)) {
                makeDisplaySymbol(marketDataSymbol, instDef.bbgCode);
            }
        }
    }

    private void makeDisplaySymbol(final String symbol, final String bbgCode) {

        final String display;
        if (bbgCode.contains(symbol)) {
            display = bbgCode;
        } else {
            display = bbgCode + " (" + symbol + ')';
        }
        final DisplaySymbol displaySymbol = new DisplaySymbol(symbol, display);
        publishIfNew(displaySymbol);
    }

    private void publishIfNew(final DisplaySymbol displaySymbol) {

        if (displaySymbols.add(displaySymbol)) {
            displaySymbolPublisher.publish(displaySymbol);
        }
    }
}
