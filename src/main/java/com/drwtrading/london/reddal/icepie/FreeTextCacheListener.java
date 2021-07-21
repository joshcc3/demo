package com.drwtrading.london.reddal.icepie;

import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.icepie.transport.data.LadderTextFreeText;
import com.drwtrading.london.reddal.SelectIOChannel;
import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;
import com.drwtrading.london.reddal.opxl.LadderTextUpdate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FreeTextCacheListener implements ITransportCacheListener<String, LadderTextFreeText> {

    private final SelectIOChannel<Collection<LadderTextUpdate>> ladderTextChannel;
    private List<LadderTextUpdate> dirty;

    public FreeTextCacheListener(final SelectIOChannel<Collection<LadderTextUpdate>> ladderTextChannel) {
        this.dirty = new ArrayList<>(20000);
        this.ladderTextChannel = ladderTextChannel;
    }

    @Override
    public boolean initialValue(final int transportID, final LadderTextFreeText item) {
        updateValue(transportID, item);
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final LadderTextFreeText item) {
        if (item.seqNum >= 0) {
            final ReddalFreeTextCell cell = ReddalFreeTextCell.getFromTransportCell(item.cell);
            final LadderTextUpdate ladderText = new LadderTextUpdate(item.symbol, cell, item.text, item.description);
            dirty.add(ladderText);
        }
        return true;
    }

    @Override
    public void batchComplete() {
        ladderTextChannel.publish(dirty);
        dirty = new ArrayList<>();
    }
}
