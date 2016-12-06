package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.SpreadContractSet;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.util.FastUtilCollections;
import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.InfoOnLadder;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.mrphil.Position;

import java.util.Map;

public class SymbolMetaData {

    public final String symbol;
    public String displaySymbol;
    public DeskPosition deskPosition;
    public InfoOnLadder infoOnLadder;
    public final Map<String, LadderText> ladderTextByPosition = FastUtilCollections.newFastMap();
    public Position dayPosition;
    public PKSExposure pksExposure;
    public String chixSwitchSymbol;
    public SpreadContractSet spreadContractSet;

    public SymbolMetaData(final String symbol) {
        this.symbol = symbol;
        this.displaySymbol = symbol;
        this.deskPosition = new DeskPosition(symbol, "");
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

    public void onDayPosition(final Position data) {
        this.dayPosition = data;
    }

    public void onPKSExposure(final PKSExposure data) {
        this.pksExposure = data;
    }

    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {
        this.displaySymbol = displaySymbol.displaySymbol;
    }

    public void setChixSwitchSymbol(final String chixSwitchSymbol) {
        this.chixSwitchSymbol = chixSwitchSymbol;
    }

    public void onFuturesContractSet(final SpreadContractSet spreadContractSet) {
        this.spreadContractSet = spreadContractSet;
    }
}