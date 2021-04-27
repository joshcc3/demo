package com.drwtrading.london.reddal.nibblers.tradingData;

import com.drwtrading.london.eeif.additiveTransport.data.AdditiveOffset;
import com.drwtrading.london.eeif.utils.transport.cache.ITransportCacheListener;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.shredders.ShredderPresenter;

public class AdditiveOffsetListener implements ITransportCacheListener<String, AdditiveOffset> {

    private final LadderPresenter ladderPresenter;
    private final ShredderPresenter shredderPresenter;

    public AdditiveOffsetListener(final LadderPresenter ladderPresenter, final ShredderPresenter shredderPresenter) {

        this.ladderPresenter = ladderPresenter;
        this.shredderPresenter = shredderPresenter;
    }

    @Override
    public boolean initialValue(final int transportID, final AdditiveOffset item) {

        updateValue(transportID, item);
        return true;
    }

    @Override
    public boolean updateValue(final int transportID, final AdditiveOffset item) {

        ladderPresenter.setAdditiveOffset(item);
        shredderPresenter.setAdditiveOffset(item);
        return true;
    }

    @Override
    public void batchComplete() {
        // no-op
    }
}