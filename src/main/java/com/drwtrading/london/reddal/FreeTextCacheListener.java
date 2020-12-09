package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.icepie.transport.data.FreeTextValue;
import com.drwtrading.london.reddal.fastui.html.FreeTextCell;
import com.drwtrading.london.reddal.opxl.LadderTextUpdate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class FreeTextCacheListener implements ITransportCacheListener<String, FreeTextValue> {

    private final TypedChannel<LadderTextUpdate> ladderTextChannel;
    private final List<LadderTextUpdate> dirty;

    public FreeTextCacheListener(final TypedChannel<LadderTextUpdate> ladderTextChannel) {
        this.dirty = new ArrayList<>(2000);
        this.ladderTextChannel = ladderTextChannel;
    }

    @Override
    public boolean initialValue(final int transportID, final FreeTextValue item) {
        updateValue(transportID, item);
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final FreeTextValue item) {
        if (item.seqNum >= 0) {
            final LadderTextUpdate ladderText =
                    new LadderTextUpdate(item.symbol, FreeTextCell.valueOf(item.cell.name()), item.text, item.description);
            dirty.add(ladderText);
        }
        return true;
    }

    @Override
    public void batchComplete() {
        for (final LadderTextUpdate ladderTextUpdate : dirty) {
            ladderTextChannel.publish(ladderTextUpdate);
        }
        dirty.clear();

    }
}
