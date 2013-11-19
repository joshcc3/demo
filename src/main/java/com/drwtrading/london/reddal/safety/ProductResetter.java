package com.drwtrading.london.reddal.safety;

import com.drwtrading.london.protocols.photon.marketdata.InstrumentDefinitionEvent;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.london.protocols.photon.marketdata.ProductReset;
import org.jetlang.channels.Publisher;

import java.util.Set;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastSet;

public class ProductResetter {
    final Set<String> symbols = newFastSet();
    private final Publisher<MarketDataEvent> fullBook;

    public ProductResetter(final Publisher<MarketDataEvent> publisher) {
        fullBook = publisher;
    }

    public void on(InstrumentDefinitionEvent msg) {
        symbols.add(msg.getSymbol());
    }

    public void reset() {
        for (String symbol : symbols) {
            fullBook.publish(new ProductReset(symbol));
        }
    }

    public Runnable resetRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                reset();
            }
        };
    }
}
