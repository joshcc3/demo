package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;
import com.drwtrading.london.reddal.opxl.LadderTextUpdate;
import com.drwtrading.london.reddal.pks.PKSExposure;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.workspace.SpreadContractSet;
import com.drwtrading.photons.ladder.LadderText;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;

public class LadderMetaData {

    public final String symbol;
    public String displaySymbol;

    public final Map<ReddalFreeTextCell, String> freeTextCells;
    public final Map<ReddalFreeTextCell, String> freeTextDescription;

    public long deskPosition;
    public String formattedDeskPosition;

    private PKSExposure pksData;
    public String pksExposure;
    public String pksPosition;

    public String chixSwitchSymbol;
    public SpreadContractSet spreadContractSet;

    public LadderMetaData(final String symbol) {

        this.symbol = symbol;
        this.displaySymbol = symbol;

        this.formattedDeskPosition = null;

        this.pksData = null;
        this.pksExposure = null;
        this.pksPosition = null;

        this.freeTextCells = new EnumMap<>(ReddalFreeTextCell.class);
        this.freeTextDescription = new EnumMap<>(ReddalFreeTextCell.class);
    }

    public void onLadderText(final LadderText ladderText) {

        final ReddalFreeTextCell cell = ReddalFreeTextCell.getCell(ladderText.getCell());
        if (null != cell) {
            this.freeTextCells.put(cell, ladderText.getText());
        }
    }

    public void setLadderText(final LadderTextUpdate ladderText) {
        this.freeTextCells.put(ladderText.cell, ladderText.text);
        this.freeTextDescription.put(ladderText.cell, ladderText.description);
    }

    public void setDeskPosition(final DecimalFormat formatter, final long position) {

        if (null == formattedDeskPosition || deskPosition != position) {

            this.deskPosition = position;
            this.formattedDeskPosition = DataUtils.formatPosition(formatter, position);
        }
    }

    public void onPKSExposure(final DecimalFormat formatter, final PKSExposure data) {

        this.pksData = data;
        final double combinedPosition = data.getCombinedPosition();

        this.pksExposure = DataUtils.formatPosition(formatter, data.dryExposure);
        this.pksPosition = DataUtils.formatPosition(formatter, combinedPosition);
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

}