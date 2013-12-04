package com.drwtrading.london.reddal.symbols;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.photons.indy.EquityIdAndSymbol;
import com.drwtrading.london.protocols.photon.marketdata.CashOutrightStructure;
import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.reddal.data.DisplaySymbol;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastSet;

public class EquityIdToDisplaySymbolMapper {

    Multimap<String, String> marketDataSymbolsByIsin = HashMultimap.create();
    Map<String, String> displaySymbolByIsin = new HashMap<String, String>();
    Set<DisplaySymbol> displaySymbols = newFastSet();
    public final Publisher<DisplaySymbol> displaySymbolPublisher;

    public EquityIdToDisplaySymbolMapper(Publisher<DisplaySymbol> displaySymbolPublisher) {
        this.displaySymbolPublisher = displaySymbolPublisher;
    }

    @Subscribe
    public void on(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        if (instrumentDefinitionEvent.getInstrumentStructure() instanceof CashOutrightStructure) {
            String isin = ((CashOutrightStructure) instrumentDefinitionEvent.getInstrumentStructure()).getIsin();
            String symbol = instrumentDefinitionEvent.getSymbol();
            marketDataSymbolsByIsin.put(isin, symbol);
            if (displaySymbolByIsin.containsKey(isin)) {
                DisplaySymbol displaySymbol = new DisplaySymbol(symbol, displaySymbolByIsin.get(isin));
                publishIfNew(displaySymbol);
            }
        }
    }

    @Subscribe
    public void on(EquityIdAndSymbol equityIdAndSymbol) {
        if (equityIdAndSymbol.isPrimary()) {
            displaySymbolByIsin.put(equityIdAndSymbol.getEquityId().getIsin(), equityIdAndSymbol.getSymbol());
            for (String marketDataSymbol : marketDataSymbolsByIsin.get(equityIdAndSymbol.getEquityId().getIsin())) {
                DisplaySymbol displaySymbol = new DisplaySymbol(marketDataSymbol, equityIdAndSymbol.getSymbol());
                publishIfNew(displaySymbol);
            }
        }
    }

    private void publishIfNew(DisplaySymbol displaySymbol) {
        if (displaySymbols.add(displaySymbol)) {
            displaySymbolPublisher.publish(displaySymbol);
        }
    }
}