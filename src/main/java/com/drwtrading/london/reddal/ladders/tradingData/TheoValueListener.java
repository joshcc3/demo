package com.drwtrading.london.reddal.ladders.tradingData;

import com.drwtrading.london.eeif.nibbler.transport.INibblerTransportConnectionListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LaserLine;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SymbolMetaData;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.reddal.ladders.LadderPresenter;

public class TheoValueListener implements INibblerTradingDataListener, INibblerTransportConnectionListener {

    private final LadderPresenter ladderPresenter;

    public TheoValueListener(final LadderPresenter ladderPresenter) {

        this.ladderPresenter = ladderPresenter;
    }

    @Override
    public boolean connectionEstablished(final String remoteAppName) {
        return true;
    }

    @Override
    public boolean addTheoValue(final TheoValue theoValue) {
        ladderPresenter.setTheo(theoValue);
        return true;
    }

    @Override
    public boolean updateTheoValue(final TheoValue theoValue) {
        ladderPresenter.setTheo(theoValue);
        return true;
    }

    @Override
    public boolean addLaserLine(final LaserLine laserLine) {
        ladderPresenter.setLaserLine(laserLine);
        return true;
    }

    @Override
    public boolean updateLaserLine(final LaserLine laserLine) {
        return true;
    }

    @Override
    public boolean addLastTrade(final LastTrade lastTrade) {
        ladderPresenter.setLastTrade(lastTrade);
        return true;
    }

    @Override
    public boolean updateLastTrade(final LastTrade lastTrade) {
        return true;
    }

    @Override
    public boolean addMetaData(final SymbolMetaData metaData) {
        ladderPresenter.setMetaData(metaData);
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

    @Override
    public void connectionLost(final String remoteAppName) {

    }
}
