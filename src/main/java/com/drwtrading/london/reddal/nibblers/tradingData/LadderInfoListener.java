package com.drwtrading.london.reddal.nibblers.tradingData;

import com.drwtrading.london.eeif.nibbler.transport.INibblerTransportConnectionListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.blotter.INibblerBlotterListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterLine;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterSymbolLine;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LaserLine;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.reddal.ladders.LadderPresenter;

public class LadderInfoListener implements INibblerTradingDataListener, INibblerTransportConnectionListener, INibblerBlotterListener {

    private final LadderPresenter ladderPresenter;

    public LadderInfoListener(final LadderPresenter ladderPresenter) {

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
    public boolean addSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        ladderPresenter.setSpreadnoughtTheo(theo);
        return true;
    }

    @Override
    public boolean updateSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        ladderPresenter.setSpreadnoughtTheo(theo);
        return true;
    }

    @Override
    public boolean addLaserLine(final LaserLine laserLine) {
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
    public boolean batchComplete() {
        return true;
    }

    @Override
    public void connectionLost(final String remoteAppName) {

    }

    @Override
    public boolean addBlotterLine(final BlotterLine blotterLine) {
        return true;
    }

    @Override
    public boolean addBlotterSymbolLine(final BlotterSymbolLine blotterLine) {
        ladderPresenter.displayTradeIssue(blotterLine.symbol, blotterLine.text);
        return true;
    }
}
