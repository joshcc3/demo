package com.drwtrading.london.reddal;

import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.channels.Publisher;

public class SubscribeToMarketData {
    public final String symbol;
    public final MemoryChannel<MarketDataEvent> marketDataEventPublisher;
    public SubscribeToMarketData(String symbol, MemoryChannel<MarketDataEvent> marketDataEventPublisher) {
        this.symbol = symbol;
        this.marketDataEventPublisher = marketDataEventPublisher;
    }
}
