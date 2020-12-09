package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.icepie.transport.data.LaserLineValue;
import com.drwtrading.london.reddal.data.LaserLine;

public class LaserLineCacheListener implements ITransportCacheListener<String, LaserLineValue> {

    private final TypedChannel<LaserLine> laserLineDataChannel;

    public LaserLineCacheListener(final TypedChannel<LaserLine> laserLineDataChannel) {
        this.laserLineDataChannel = laserLineDataChannel;
    }

    @Override
    public boolean initialValue(final int transportID, final LaserLineValue item) {
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final LaserLineValue item) {
        final LaserLine laserData;
        if (item.seqNum >= 0) {
             laserData = new LaserLine(item.symbol, item.type, item.value);
        } else {
            laserData = new LaserLine(item.symbol, item.type);
        }
        laserLineDataChannel.publish(laserData);
        return true;
    }

    @Override
    public void batchComplete() {

    }
}
