package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.safety.TradingStatusWatchdog;

import java.util.Map;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class TradingStatusForAll {
    public Map<String, TradingStatusWatchdog.ServerTradingStatus> serverTradingStatusMap = newFastMap();
    public void on(TradingStatusWatchdog.ServerTradingStatus tradingStatus) {
        serverTradingStatusMap.put(tradingStatus.server, tradingStatus);
    }
}
