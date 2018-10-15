package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportConnected;

import java.util.HashSet;
import java.util.Set;

public class TradingStatusForAll {

    private final Set<String> connectedNibblers;

    public TradingStatusForAll() {
        this.connectedNibblers = new HashSet<>();
    }

    public void setNibblerConnected(final NibblerTransportConnected tradingStatus) {

        if (tradingStatus.isConnected) {
            connectedNibblers.add(tradingStatus.nibblerName);
        } else {
            connectedNibblers.remove(tradingStatus.nibblerName);
        }
    }

    public boolean isNibblerConnected(final String nibblerName) {
        return connectedNibblers.contains(nibblerName);
    }
}
