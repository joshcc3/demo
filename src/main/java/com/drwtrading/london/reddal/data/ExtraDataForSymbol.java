package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.util.FastUtilCollections;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.photons.ladder.LastTrade;
import com.drwtrading.photons.ladder.Side;

import java.util.Map;

public class ExtraDataForSymbol {

    public final String symbol;
    public final Map<String, LaserLine> laserLineByName = FastUtilCollections.newFastMap();
    public LastTrade lastBuy;
    public LastTrade lastSell;
    public boolean symbolAvailable = false;

    public ExtraDataForSymbol(final String symbol) {
        this.symbol = symbol;
    }

    public void onLaserLine(final LaserLine laserLine) {
        laserLineByName.put(laserLine.getId(), laserLine);
    }

    public void onLastTrade(final LastTrade lastTrade) {
        if (lastTrade.getSide() == Side.BID) {
            lastBuy = lastTrade;
        } else if (lastTrade.getSide() == Side.OFFER) {
            lastSell = lastTrade;
        }
    }

    public void setSymbolAvailable() {
        symbolAvailable = true;
    }
}