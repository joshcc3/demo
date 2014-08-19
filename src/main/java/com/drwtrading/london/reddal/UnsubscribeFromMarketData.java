package com.drwtrading.london.reddal;

import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import org.jetlang.channels.MemoryChannel;

public class UnsubscribeFromMarketData {
    public final String symbol;
    public final MemoryChannel<MarketDataEvent> marketDataEventPublisher;
    public UnsubscribeFromMarketData(String symbol, MemoryChannel<MarketDataEvent> marketDataEventPublisher) {
        this.symbol = symbol;
        this.marketDataEventPublisher = marketDataEventPublisher;
    }
}
