package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingNotificationsHandler;

public final class NibblerNotificationHandler implements INibblerTradingNotificationsHandler {

    public static final NibblerNotificationHandler INSTANCE = new NibblerNotificationHandler();

    private NibblerNotificationHandler() {
        // singleton
    }

    @Override
    public void submitAcknowledged(final int clOrdID, final int chainID) {
        System.out.println(clOrdID + " -> " + chainID);
    }

    @Override
    public void submitFullyFilled(final int clOrdID, final int chainID) {

        System.out.println("Filled: " + clOrdID + " -> " + chainID);
    }

    @Override
    public void submitRejected(final int clOrdID) {
        System.out.println("Reject: " + clOrdID);
    }
}
