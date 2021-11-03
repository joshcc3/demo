package com.drwtrading.london.reddal.icepie;

import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.icepie.transport.data.LadderTextColour;
import com.drwtrading.london.reddal.SelectIOChannel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LadderTextColourCacheListener implements ITransportCacheListener<String, LadderTextColour> {

    private final SelectIOChannel<Collection<LadderTextColour>> ladderColourChannel;
    private List<LadderTextColour> dirty;

    public LadderTextColourCacheListener(final SelectIOChannel<Collection<LadderTextColour>> ladderColourChannel) {
        this.dirty = new ArrayList<>(20000);
        this.ladderColourChannel = ladderColourChannel;
    }

    @Override
    public boolean initialValue(final int transportID, final LadderTextColour item) {
        updateValue(transportID, item);
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final LadderTextColour item) {
        if (item.seqNum >= 0) {
            final LadderTextColour copy = item.getCacheCopy();
            copy.set(item);
            dirty.add(copy);
        }
        return true;
    }

    @Override
    public void batchComplete() {
        ladderColourChannel.publish(dirty);
        dirty = new ArrayList<>();
    }
}
