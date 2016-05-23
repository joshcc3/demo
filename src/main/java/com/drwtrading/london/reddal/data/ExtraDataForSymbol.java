package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.SpreadContractSet;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.util.FastUtilCollections;
import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.InfoOnLadder;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.drwtrading.photons.ladder.LastTrade;
import com.drwtrading.photons.ladder.Side;
import com.drwtrading.photons.mrphil.Position;

import java.util.Map;

public class ExtraDataForSymbol {

    public final String symbol;
    public String displaySymbol;
    public final Map<String, LaserLine> laserLineByName = FastUtilCollections.newFastMap();
    public DeskPosition deskPosition;
    public InfoOnLadder infoOnLadder;
    public final Map<String, LadderText> ladderTextByPosition = FastUtilCollections.newFastMap();
    public LastTrade lastBuy;
    public LastTrade lastSell;
    public Position dayPosition;
    public boolean symbolAvailable = false;
    public SpreadContractSet spreadContractSet;
    public String chixSwitchSymbol;

    public ExtraDataForSymbol(final String symbol) {
        this.symbol = symbol;
        this.displaySymbol = symbol;
        this.deskPosition = new DeskPosition(symbol, "");
    }

    public void onLaserLine(final LaserLine laserLine) {
        laserLineByName.put(laserLine.getId(), laserLine);
    }

    public void onDeskPosition(final DeskPosition deskPosition) {
        this.deskPosition = deskPosition;
    }

    public void onInfoOnLadder(final InfoOnLadder infoOnLadder) {
        this.infoOnLadder = infoOnLadder;
    }

    public void onLadderText(final LadderText ladderText) {
        this.ladderTextByPosition.put(ladderText.getCell(), ladderText);
    }

    public void onLastTrade(final LastTrade lastTrade) {
        if (lastTrade.getSide() == Side.BID) {
            lastBuy = lastTrade;
        } else if (lastTrade.getSide() == Side.OFFER) {
            lastSell = lastTrade;
        }
    }

    public void onDayPosition(final Position data) {
        this.dayPosition = data;
    }

    public void setSymbolAvailable() {
        symbolAvailable = true;
    }

    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {
        this.displaySymbol = displaySymbol.displaySymbol;
    }

    public void onFuturesContractSet(final SpreadContractSet spreadContractSet) {
        this.spreadContractSet = spreadContractSet;
    }

    public void setChixSwitchSymbol(final String chixSwitchSymbol) {
        this.chixSwitchSymbol = chixSwitchSymbol;
    }
}
