package com.drwtrading.london.reddal.symbols;

import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.reddal.opxl.UltimateParentMapping;
import org.jetlang.channels.Publisher;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class DisplaySymbolMapper {

    private final Publisher<DisplaySymbol> displaySymbolPublisher;

    private final Map<String, HashSet<String>> mdSymbolsByIsin;
    private final Map<String, String> bbgByIsin;
    private final Set<DisplaySymbol> displaySymbols;

    private final Map<String, String> ultimateParents;

    private final Map<String, String> ultimateParentBBG;

    public DisplaySymbolMapper(final Publisher<DisplaySymbol> displaySymbolPublisher) {

        this.displaySymbolPublisher = displaySymbolPublisher;

        this.mdSymbolsByIsin = new HashMap<>();
        this.bbgByIsin = new HashMap<>();
        this.displaySymbols = new HashSet<>();

        this.ultimateParents = new HashMap<>();
        this.ultimateParentBBG = new HashMap<>();
    }

    public void setSearchResult(final SearchResult searchResult) {

        switch (searchResult.instType) {
            case EQUITY:
            case DR:
            case ETF: {
                final Set<String> symbols = MapUtils.getMappedSet(mdSymbolsByIsin, searchResult.instID.isin);
                symbols.add(searchResult.symbol);
                makeDisplaySymbol(searchResult.instID.isin);
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

            for (final Map.Entry<String, String> ultimateParent : ultimateParents.entrySet()) {

                if (ultimateParent.getValue().equals(instDef.instID.isin)) {
                    ultimateParentBBG.put(ultimateParent.getKey(), instDef.bbgCode);
                }
            }

            if (!ultimateParents.containsKey(instDef.instID.isin)) {
                ultimateParentBBG.put(instDef.instID.isin, instDef.bbgCode);
            }
            makeDisplaySymbol(instDef.instID.isin);
        }
    }

    public void setUltimateParent(final UltimateParentMapping ultimateParent) {

        ultimateParents.put(ultimateParent.childISIN, ultimateParent.parentID.isin);

        final String parentBBGCode = bbgByIsin.get(ultimateParent.parentID.isin);
        if (null != parentBBGCode) {
            ultimateParentBBG.put(ultimateParent.childISIN, parentBBGCode);
            makeDisplaySymbol(ultimateParent.childISIN);
        }
    }

    private void makeDisplaySymbol(final String isin) {

        final Set<String> symbols = mdSymbolsByIsin.get(isin);

        if (null != symbols) {
            final String ultimateBBG = ultimateParentBBG.get(isin);

            for (final String symbol : mdSymbolsByIsin.get(isin)) {

                final String display;
                if (null == ultimateBBG || ultimateBBG.contains(symbol)) {
                    display = symbol;
                } else {
                    display = ultimateBBG + " (" + symbol + ')';
                }

                final DisplaySymbol displaySymbol = new DisplaySymbol(symbol, display);
                publishIfNew(displaySymbol);
            }
        }
    }

    private void publishIfNew(final DisplaySymbol displaySymbol) {

        if (displaySymbols.add(displaySymbol)) {
            displaySymbolPublisher.publish(displaySymbol);
        }
    }
}
