package com.drwtrading.london.reddal.stockAlerts.yoda;

import com.drwtrading.london.eeif.utils.config.ConfigException;
import com.drwtrading.london.eeif.utils.config.ConfigGroup;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.eeif.yoda.transport.YodaSignalType;
import com.drwtrading.london.eeif.yoda.transport.YodaTransportComponents;
import com.drwtrading.london.eeif.yoda.transport.cache.YodaClientCacheFactory;
import com.drwtrading.london.eeif.yoda.transport.io.YodaClientHandler;
import com.drwtrading.london.reddal.stockAlerts.StockAlert;
import org.jetlang.channels.Publisher;

import java.util.EnumSet;

public class YodaSignals {

    public void setupYodaSignals(final SelectIO selectIO, final IResourceMonitor<YodaTransportComponents> monitor, final ConfigGroup config,
            final String appName, final Publisher<StockAlert> stockAlerts) throws ConfigException {

        final YodaRestingOrderClient restingClient = new YodaRestingOrderClient(stockAlerts);
        final YodaSweepClient sweepClient = new YodaSweepClient(stockAlerts);
        final YodaTWAPClient twapClient = new YodaTWAPClient(stockAlerts);

        final YodaClientHandler yodaHandler =
                YodaClientCacheFactory.createClientCache(selectIO, monitor, "yoda", appName, restingClient, sweepClient, twapClient,
                        EnumSet.allOf(YodaSignalType.class));

        YodaClientCacheFactory.createClient(selectIO, config, monitor, yodaHandler);
    }
}
