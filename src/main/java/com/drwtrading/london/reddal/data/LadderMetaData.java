package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.fastui.html.FreeTextCell;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.InfoOnLadder;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.mrphil.Position;

import java.util.EnumMap;
import java.util.Map;

public class LadderMetaData {

    public final String symbol;
    public String displaySymbol;
    public DeskPosition deskPosition;
    public InfoOnLadder infoOnLadder;
    public final Map<FreeTextCell, String> freeTextCells;
    public Position dayPosition;
    public PKSExposure pksExposure;
    public String chixSwitchSymbol;
    public SpreadContractSet spreadContractSet;

    public LadderMetaData(final String symbol) {
        this.symbol = symbol;
        this.displaySymbol = symbol;
        this.deskPosition = new DeskPosition(symbol, "");
        this.freeTextCells = new EnumMap<>(FreeTextCell.class);
    }

    public void onDeskPosition(final DeskPosition deskPosition) {
        this.deskPosition = deskPosition;
    }

    public void onInfoOnLadder(final InfoOnLadder infoOnLadder) {
        this.infoOnLadder = infoOnLadder;
    }

    public void onLadderText(final LadderText ladderText) {

        final FreeTextCell cell = FreeTextCell.getCell(ladderText.getCell());
        if (null != cell) {
            this.freeTextCells.put(cell, ladderText.getText());
        }
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

    public void onSpreadContractSet(final SpreadContractSet spreadContractSet) {

        if (null == this.spreadContractSet || spreadContractSet.symbol.equals(symbol)) {
            this.spreadContractSet = spreadContractSet;
        }
    }
}