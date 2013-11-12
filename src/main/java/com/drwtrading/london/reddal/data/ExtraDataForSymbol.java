package com.drwtrading.london.reddal.data;

import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.InfoOnLadder;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;

import java.util.HashMap;
import java.util.Map;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class ExtraDataForSymbol {
    public final String symbol;
    public final Map<String, LaserLine> laserLineByName = newFastMap();
    public DeskPosition deskPosition;
    public InfoOnLadder infoOnLadder;
    public final Map<String, LadderText> ladderTextByPosition = newFastMap();
    public ExtraDataForSymbol(String symbol) {
        this.symbol = symbol;
        deskPosition = new DeskPosition(symbol, "");
    }
    public void onLaserLine(LaserLine laserLine) {
        laserLineByName.put(laserLine.getId(), laserLine);
    }
    public void onDeskPosition(DeskPosition deskPosition) {
        this.deskPosition = deskPosition;
    }

    public void onInfoOnLadder(InfoOnLadder infoOnLadder) {
        this.infoOnLadder = infoOnLadder;
    }
    public void onLadderText(LadderText ladderText) {
        this.ladderTextByPosition.put(ladderText.getCell(), ladderText);
    }
}
