package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingNotificationsHandler;

public final class NibblerNotificationHandler implements INibblerTradingNotificationsHandler {

    public static final NibblerNotificationHandler INSTANCE = new NibblerNotificationHandler();

    private NibblerNotificationHandler() {
        // singleton
    }

    @Override
    public void submitAcknowledged(final int clOrdID, final int chainID) {
        // no-op
    }

    @Override
    public void submitFullyFilled(final int clOrdID, final int chainID) {
        // no-op
    }

    @Override
    public void submitRejected(final int clOrdID) {
        // no-op
    }
}
