package com.drwtrading.london.reddal.symbols;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.indy.transport.data.InstrumentDef;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.FutureOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.reddal.data.DisplaySymbol;
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

    private final Multimap<String, String> marketDataSymbolsByIsin = HashMultimap.create();
    private final Map<String, String> displaySymbolByIsin = new HashMap<>();
    private final Set<DisplaySymbol> displaySymbols = new HashSet<>();

    private final Publisher<DisplaySymbol> displaySymbolPublisher;

    public DisplaySymbolMapper(final Publisher<DisplaySymbol> displaySymbolPublisher) {
        this.displaySymbolPublisher = displaySymbolPublisher;
    }

    public void setInstDefEvent(final InstrumentDefinitionEvent instrumentDefinitionEvent) {

        if (instrumentDefinitionEvent.getInstrumentStructure() instanceof CashOutrightStructure) {
            final String isin = ((CashOutrightStructure) instrumentDefinitionEvent.getInstrumentStructure()).getIsin();
            final String symbol = instrumentDefinitionEvent.getSymbol();
            marketDataSymbolsByIsin.put(isin, symbol);
            if (displaySymbolByIsin.containsKey(isin)) {
                final String display = displaySymbolByIsin.get(isin);
                final DisplaySymbol displaySymbol = new DisplaySymbol(symbol, makeDisplaySymbol(symbol, display));
                publishIfNew(displaySymbol);
            }
        } else if (instrumentDefinitionEvent.getInstrumentStructure() instanceof FutureOutrightStructure) {
            final String market = instrumentDefinitionEvent.getMarket();
            final SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM yy");
            final String expiry = simpleDateFormat.format(
                    new Date(((FutureOutrightStructure) instrumentDefinitionEvent.getInstrumentStructure()).getExpiry().getTimestamp()));
            publishIfNew(new DisplaySymbol(instrumentDefinitionEvent.getSymbol(), market + ' ' + expiry));
        }
    }

    public void setInstDef(final InstrumentDef instDef) {
        if (instDef.isPrimary) {
            displaySymbolByIsin.put(instDef.instID.isin, instDef.bbgCode);
            for (final String marketDataSymbol : marketDataSymbolsByIsin.get(instDef.instID.isin)) {
                final DisplaySymbol displaySymbol =
                        new DisplaySymbol(marketDataSymbol, makeDisplaySymbol(marketDataSymbol, instDef.bbgCode));
                publishIfNew(displaySymbol);
            }
        }
    }

    private void publishIfNew(final DisplaySymbol displaySymbol) {
        if (displaySymbols.add(displaySymbol)) {
            displaySymbolPublisher.publish(displaySymbol);
        }
    }

    private static String makeDisplaySymbol(final String symbol, String display) {
        if (!display.contains(symbol)) {
            display = display + " (" + symbol + ')';
        }
        return display;
    }
}
