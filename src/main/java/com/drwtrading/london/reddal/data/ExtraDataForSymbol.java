package com.drwtrading.london.reddal.data;

import com.drwtrading.photons.ladder.DeskPosition;
import com.drwtrading.photons.ladder.LaserLine;

import java.util.HashMap;
import java.util.Map;

public class ExtraDataForSymbol {
    public final String symbol;
    public final Map<String, LaserLine> laserLineByName = new HashMap<String, LaserLine>();
    public DeskPosition deskPosition;

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
}
