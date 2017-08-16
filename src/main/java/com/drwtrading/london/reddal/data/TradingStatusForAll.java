package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.safety.ServerTradingStatus;

import java.util.Map;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class TradingStatusForAll {

    public Map<String, ServerTradingStatus> serverTradingStatusMap = newFastMap();

    public TradingStatusForAll() {
    }

    public void on(final ServerTradingStatus tradingStatus) {
        serverTradingStatusMap.put(tradingStatus.server, tradingStatus);
    }
}
