package com.drwtrading.london.reddal.pks;

import com.drwtrading.london.eeif.position.transport.cache.IPositionCmdListener;
import com.drwtrading.london.eeif.position.transport.data.ConstituentExposure;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;

public class PksClient implements ITransportCacheListener<String, ConstituentExposure>, IPositionCmdListener {

    @Override
    public boolean setHedgingEnabled(final boolean isHedgingEnabled) {
        return true;
    }

    @Override
    public boolean initialValue(final int transportID, final ConstituentExposure item) {
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final ConstituentExposure item) {
        return true;
    }

    @Override
    public void batchComplete() {

    }
}
