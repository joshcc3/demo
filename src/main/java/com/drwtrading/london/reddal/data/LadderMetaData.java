package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.fastui.html.FreeTextCell;
import com.drwtrading.london.reddal.opxl.OpxlLadderText;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.mrphil.Position;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;

public class LadderMetaData {

    public final String symbol;
    public String displaySymbol;

    public final Map<FreeTextCell, String> freeTextCells;

    public long deskPosition;
    public String formattedDeskPosition;

    public long mrPhilNetPosition;
    public String formattedMrPhilNetPosition;
    public String formattedMrPhilVolume;

    private PKSExposure pksData;
    public String pksExposure;
    public String pksPosition;

    public String chixSwitchSymbol;
    public SpreadContractSet spreadContractSet;

    public LadderMetaData(final String symbol) {

        this.symbol = symbol;
        this.displaySymbol = symbol;

        this.formattedDeskPosition = null;
        this.formattedMrPhilNetPosition = null;
        this.formattedMrPhilVolume = null;

        this.pksData = null;
        this.pksExposure = null;
        this.pksPosition = null;

        this.freeTextCells = new EnumMap<>(FreeTextCell.class);
    }

    public void onLadderText(final LadderText ladderText) {

        final FreeTextCell cell = FreeTextCell.getCell(ladderText.getCell());
        if (null != cell) {
            this.freeTextCells.put(cell, ladderText.getText());
        } else if ("r3c1".equals(ladderText.getCell())) {
            this.pksExposure = ladderText.getText();
        } else if ("r3c5".equals(ladderText.getCell())) {
            this.pksPosition = ladderText.getText();
        }
    }

    public void setLadderText(final OpxlLadderText ladderText) {
        this.freeTextCells.put(ladderText.cell, ladderText.text);
    }

    public void setDeskPosition(final DecimalFormat formatter, final long position) {

        if (null == formattedDeskPosition || deskPosition != position) {

            this.deskPosition = position;
            this.formattedDeskPosition = formatPosition(formatter, position);
        }
    }

    public void setMrPhilPosition(final DecimalFormat formatter, final Position mrPhilPosition) {

        if (null == formattedMrPhilNetPosition || mrPhilNetPosition != mrPhilPosition.getNet()) {

            this.mrPhilNetPosition = mrPhilPosition.getNet();
            this.formattedMrPhilNetPosition = formatPosition(formatter, mrPhilNetPosition);
            this.formattedMrPhilVolume = formatPosition(formatter, mrPhilPosition.getVolume());
        }
    }

    public void onPKSExposure(final DecimalFormat formatter, final PKSExposure data) {

        this.pksData = data;
        this.pksExposure = formatPosition(formatter, data.exposure);
        this.pksPosition = formatPosition(formatter, data.position);
    }

    public PKSExposure getPKSData() {
        return pksData;
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

    private static String formatPosition(final DecimalFormat formatter, final double qty) {

        final double absQty = Math.abs(qty);
        if (absQty < 10000) {
            return Integer.toString((int) qty);
        } else if (absQty < 1000000) {
            return formatter.format(qty / 1000.0) + 'K';
        } else {
            return formatter.format(qty / 1000000.0) + 'M';
        }
    }
}