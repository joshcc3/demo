package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.builder.FiberGroup;
import com.drwtrading.london.protocols.photon.marketdata.MarketDataEvent;
import com.drwtrading.marketdata.service.snapshot.publishing.SnapshottingPublisher;
import org.jetlang.channels.MemoryChannel;
import org.jetlang.core.Disposable;

import java.util.HashMap;
import java.util.Map;

public class MarketDataSubscriber {


    final SnapshottingPublisher<MarketDataEvent> marketDataEventSnapshottingPublisher;
    Map<MemoryChannel<MarketDataEvent>, Disposable> subscriptions = new HashMap<>();


    public MarketDataSubscriber(SnapshottingPublisher<MarketDataEvent> marketDataEventSnapshottingPublisher) {
        this.marketDataEventSnapshottingPublisher = marketDataEventSnapshottingPublisher;
    }

    @Subscribe
    public void on(SubscribeToMarketData subscribeToMarketData) {
        Disposable disposable = marketDataEventSnapshottingPublisher.subscribe(subscribeToMarketData.symbol, subscribeToMarketData.marketDataEventPublisher);
        Disposable previous = subscriptions.put(subscribeToMarketData.marketDataEventPublisher, disposable);
        if (previous != null) {
            previous.dispose();
            throw new IllegalArgumentException("Double-subscription to market data symbol " + subscribeToMarketData.symbol);
        }
    }

    @Subscribe
    public void on(UnsubscribeFromMarketData unsubscribeFromMarketData) {
        Disposable disposable = subscriptions.get(unsubscribeFromMarketData.marketDataEventPublisher);
        if (disposable != null) {
            disposable.dispose();
        }
    }

}
