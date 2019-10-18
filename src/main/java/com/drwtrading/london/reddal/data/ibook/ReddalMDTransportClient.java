package com.drwtrading.london.reddal.data.ibook;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.MDTransportComponents;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.io.MDTransportClient;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.units.MDTransportLevel2UnitHandler;
import com.drwtrading.london.eeif.utils.marketData.transport.tcpShaped.units.MDTransportLevel3UnitHandler;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;

public class ReddalMDTransportClient extends MDTransportClient {

    private final LevelThreeBookSubscriber l3UnitHandler;
    private final LevelTwoBookSubscriber l2UnitHandler;

    public ReddalMDTransportClient(final SelectIO selectIO, final IResourceMonitor<MDTransportComponents> monitor, final MDSource mdSource,
            final String appName, final LevelThreeBookSubscriber l3UnitHandler, final LevelTwoBookSubscriber l2UnitHandler,
            final int msgMaxTimeDiffMillis, final boolean isTradeSnapshotWanted) {

        super(selectIO, monitor, mdSource.name(), appName, getL3UnitHandler(mdSource, l3UnitHandler),
                getL2UnitHandler(mdSource, l2UnitHandler), msgMaxTimeDiffMillis, isTradeSnapshotWanted);

        this.l3UnitHandler = l3UnitHandler;
        this.l2UnitHandler = l2UnitHandler;
    }

    private static MDTransportLevel3UnitHandler getL3UnitHandler(final MDSource mdSource, final LevelThreeBookSubscriber l3BookHandler) {

        return new MDTransportLevel3UnitHandler(l3BookHandler, mdSource);
    }

    private static MDTransportLevel2UnitHandler getL2UnitHandler(final MDSource mdSource, final LevelTwoBookSubscriber l2BookHandler) {

        return new MDTransportLevel2UnitHandler(l2BookHandler, mdSource);
    }

    @Override
    public void closed() {

        super.closed();
        l3UnitHandler.closed();
        l2UnitHandler.closed();
    }
}
