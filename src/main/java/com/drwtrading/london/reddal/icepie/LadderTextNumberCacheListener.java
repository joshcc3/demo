package com.drwtrading.london.reddal.icepie;

import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.icepie.transport.io.LadderTextNumber;
import com.drwtrading.london.reddal.SelectIOChannel;
import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;
import com.drwtrading.london.reddal.opxl.LadderNumberUpdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LadderTextNumberCacheListener implements ITransportCacheListener<String, LadderTextNumber> {

    private final SelectIOChannel<Collection<LadderNumberUpdate>> ladderNumberChannel;
    private List<LadderNumberUpdate> dirty;

    public LadderTextNumberCacheListener(final SelectIOChannel<Collection<LadderNumberUpdate>> ladderNumberChannel) {
        this.dirty = new ArrayList<>(20000);
        this.ladderNumberChannel = ladderNumberChannel;
    }

    @Override
    public boolean initialValue(final int transportID, final LadderTextNumber item) {
        updateValue(transportID, item);
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final LadderTextNumber item) {
        if (item.seqNum >= 0) {
            final ReddalFreeTextCell cell = ReddalFreeTextCell.getFromTransportCell(item.cell);
            final LadderNumberUpdate ladderText = new LadderNumberUpdate(item.symbol, cell, item.value, item.units, item.description);
            dirty.add(ladderText);
        }
        return true;
    }

    @Override
    public void batchComplete() {
        ladderNumberChannel.publish(dirty);
        dirty = new ArrayList<>();
    }
}
