package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.icepie.transport.data.LaserLineValue;
import com.drwtrading.london.reddal.data.LaserLine;
import com.drwtrading.london.reddal.opxl.LadderTextUpdate;

import java.util.LinkedList;
import java.util.List;

public class LaserLineCacheListener implements ITransportCacheListener<String, LaserLineValue> {

    private final TypedChannel<LaserLine> laserLineDataChannel;
    private final List<LaserLine> dirty;

    public LaserLineCacheListener(final TypedChannel<LaserLine> laserLineDataChannel) {
        this.dirty = new LinkedList<>();
        this.laserLineDataChannel = laserLineDataChannel;
    }

    @Override
    public boolean initialValue(final int transportID, final LaserLineValue item) {
        updateValue(transportID, item);
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
        dirty.add(laserData);
        return true;
    }

    @Override
    public void batchComplete() {
        for (final LaserLine laserLine : dirty) {
            laserLineDataChannel.publish(laserLine);
        }
        dirty.clear();
    }
}
