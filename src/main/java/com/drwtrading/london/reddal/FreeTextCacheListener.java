package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.icepie.transport.data.FreeTextValue;
import com.drwtrading.london.reddal.fastui.html.FreeTextCell;
import com.drwtrading.london.reddal.opxl.LadderTextUpdate;

public class FreeTextCacheListener implements ITransportCacheListener<String, FreeTextValue> {

    private final TypedChannel<LadderTextUpdate> ladderTextChannel;

    public FreeTextCacheListener(final TypedChannel<LadderTextUpdate> ladderTextChannel) {
        this.ladderTextChannel = ladderTextChannel;
    }

    @Override
    public boolean initialValue(final int transportID, final FreeTextValue item) {
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final FreeTextValue item) {
        if (item.seqNum >= 0) {
            final LadderTextUpdate ladderText =
                    new LadderTextUpdate(item.symbol, FreeTextCell.valueOf(item.cell.name()), item.text, item.description);
            ladderTextChannel.publish(ladderText);
        }
        return true;
    }

    @Override
    public void batchComplete() {

    }
}
