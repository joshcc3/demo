package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LaserLine;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SymbolMetaData;
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
    public boolean addLaserLine(final LaserLine laserLine) {
        shredderPresenter.setLaserLine(laserLine);
        return true;
    }

    @Override
    public boolean updateLaserLine(final LaserLine laserLine) {
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
    public boolean addMetaData(final SymbolMetaData metaData) {
        return true;
    }

    @Override
    public boolean updateMetaData(final SymbolMetaData metaData) {
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }
}
