package com.drwtrading.london.reddal.symbols;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.photons.indy.EquityIdAndSymbol;
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
import java.util.Map;
import java.util.Set;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastSet;

public class DisplaySymbolMapper {

    Multimap<String, String> marketDataSymbolsByIsin = HashMultimap.create();
    Map<String, String> displaySymbolByIsin = new HashMap<>();
    Set<DisplaySymbol> displaySymbols = newFastSet();
    public final Publisher<DisplaySymbol> displaySymbolPublisher;

    public DisplaySymbolMapper(Publisher<DisplaySymbol> displaySymbolPublisher) {
        this.displaySymbolPublisher = displaySymbolPublisher;
    }

    @Subscribe
    public void on(InstrumentDefinitionEvent instrumentDefinitionEvent) {
        if (instrumentDefinitionEvent.getInstrumentStructure() instanceof CashOutrightStructure) {
            String isin = ((CashOutrightStructure) instrumentDefinitionEvent.getInstrumentStructure()).getIsin();
            String symbol = instrumentDefinitionEvent.getSymbol();
            marketDataSymbolsByIsin.put(isin, symbol);
            if (displaySymbolByIsin.containsKey(isin)) {
                String display = displaySymbolByIsin.get(isin);
                DisplaySymbol displaySymbol = new DisplaySymbol(symbol, makeDisplaySymbol(symbol, display));
                publishIfNew(displaySymbol);
            }
        } else if (instrumentDefinitionEvent.getInstrumentStructure() instanceof FutureOutrightStructure) {
            String market = instrumentDefinitionEvent.getMarket();
            SimpleDateFormat simpleDateFormat = new SimpleDateFormat("MMM yy");
            String expiry = simpleDateFormat.format(new Date(((FutureOutrightStructure) instrumentDefinitionEvent.getInstrumentStructure()).getExpiry().getTimestamp()));
            publishIfNew(new DisplaySymbol(instrumentDefinitionEvent.getSymbol(), market + " " + expiry));
        }
    }

    private String makeDisplaySymbol(String symbol, String display) {
        if (!display.contains(symbol)) {
            display = display + " (" + symbol + ")";
        }
        return display;
    }

    @Subscribe
    public void on(EquityIdAndSymbol equityIdAndSymbol) {
        if (equityIdAndSymbol.isPrimary()) {
            displaySymbolByIsin.put(equityIdAndSymbol.getEquityId().getIsin(), equityIdAndSymbol.getSymbol());
            for (String marketDataSymbol : marketDataSymbolsByIsin.get(equityIdAndSymbol.getEquityId().getIsin())) {
                DisplaySymbol displaySymbol = new DisplaySymbol(marketDataSymbol, makeDisplaySymbol(marketDataSymbol, equityIdAndSymbol.getSymbol()));
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
