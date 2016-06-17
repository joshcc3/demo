package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.indy.transport.data.InstrumentDef;
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

    public void setSearchResult(final SearchResult searchResult) {

        switch (searchResult.instType) {
            case EQUITY:
            case DR:
            case ETF: {
                mdSymbolsByIsin.put(searchResult.instID.isin, searchResult.symbol);
                final String bbgCode = bbgByIsin.get(searchResult.instID.isin);
                if (null != bbgCode) {
                    makeDisplaySymbol(searchResult.symbol, bbgCode);
                }
                break;
            }
            case FUTURE: {

                final String market = searchResult.symbol.substring(0, searchResult.symbol.length() - 2);

                final SimpleDateFormat sdf = new SimpleDateFormat("MMM yy");
                final Date expiryTime = new Date(searchResult.expiry);
                final String expiry = sdf.format(expiryTime);

                final DisplaySymbol displaySymbol = new DisplaySymbol(searchResult.symbol, market + ' ' + expiry);
                publishIfNew(displaySymbol);
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
