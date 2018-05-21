package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;

public class ShredderInfoListener implements INibblerTradingDataListener {

    private final ShredderPresenter shredderPresenter;

    public ShredderInfoListener(final ShredderPresenter shredderPresenter) {
        this.shredderPresenter = shredderPresenter;
    }

    @Override
    public boolean addTheoValue(final TheoValue theoValue) {
        shredderPresenter.setTheo(theoValue);
        return true;
    }

    @Override
    public boolean updateTheoValue(final TheoValue theoValue) {
        shredderPresenter.setTheo(theoValue);
        return true;
    }

    @Override
    public boolean addSpreadnoughtTheo(final SpreadnoughtTheo theoValue) {
        shredderPresenter.setSpreadnoughtTheo(theoValue);
        return true;
    }

    @Override
    public boolean updateSpreadnoughtTheo(final SpreadnoughtTheo theoValue) {
        shredderPresenter.setSpreadnoughtTheo(theoValue);
        return true;
    }

    @Override
    public boolean addLastTrade(final LastTrade lastTrade) {
        return true;
    }

    @Override
    public boolean updateLastTrade(final LastTrade lastTrade) {
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }
}
