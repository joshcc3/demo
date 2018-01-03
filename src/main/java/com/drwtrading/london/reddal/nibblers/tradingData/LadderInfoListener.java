package com.drwtrading.london.reddal.nibblers.tradingData;

import com.drwtrading.london.eeif.nibbler.transport.INibblerTransportConnectionListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.blotter.INibblerBlotterListener;
import com.drwtrading.london.eeif.nibbler.transport.cache.tradingData.INibblerTradingDataListener;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterLine;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterSymbolLine;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LaserLine;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LastTrade;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.picard.PicardSpotter;

public class LadderInfoListener implements INibblerTradingDataListener, INibblerTransportConnectionListener, INibblerBlotterListener {

    private final LadderPresenter ladderPresenter;
    private final PicardSpotter picardSpotter;

    public LadderInfoListener(final LadderPresenter ladderPresenter, final PicardSpotter picardSpotter) {

        this.ladderPresenter = ladderPresenter;
        this.picardSpotter = picardSpotter;
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
        picardSpotter.setLaserLine(laserLine);
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
