package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;
import com.drwtrading.london.reddal.opxl.LadderNumberUpdate;
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
    public final Map<ReddalFreeTextCell, LadderNumberUpdate> ladderTextNumberCells;
    public final Map<ReddalFreeTextCell, String> ladderTextDescription;

    public final LazilyFormattedMetadataLong deskPosition;

    private PKSExposure pksData;
    public final LazilyFormattedMetadataDouble pksExposure;
    public final LazilyFormattedMetadataDouble pksPosition;

    public String chixSwitchSymbol;
    public SpreadContractSet spreadContractSet;

    public LadderMetaData(final String symbol) {

        this.symbol = symbol;
        this.displaySymbol = symbol;

        this.deskPosition = new LazilyFormattedMetadataLong();
        this.pksData = null;
        this.pksExposure = new LazilyFormattedMetadataDouble();
        this.pksPosition = new LazilyFormattedMetadataDouble();

        this.freeTextCells = new EnumMap<>(ReddalFreeTextCell.class);
        this.ladderTextNumberCells = new EnumMap<>(ReddalFreeTextCell.class);
        this.ladderTextDescription = new EnumMap<>(ReddalFreeTextCell.class);
    }

    public void onLadderText(final LadderText ladderText) {

        final ReddalFreeTextCell cell = ReddalFreeTextCell.getCell(ladderText.getCell());
        if (null != cell) {
            this.freeTextCells.put(cell, ladderText.getText());
        }
    }

    public void setLadderText(final LadderTextUpdate ladderText) {
        final ReddalFreeTextCell cell = ladderText.cell;
        this.freeTextCells.put(cell, ladderText.text);
        this.ladderTextNumberCells.remove(cell);
        this.ladderTextDescription.put(cell, ladderText.description);
    }

    public void setLadderNumber(final LadderNumberUpdate ladderNumber) {
        final ReddalFreeTextCell cell = ladderNumber.cell;
        this.ladderTextNumberCells.put(cell, ladderNumber);
        this.freeTextCells.remove(cell);
        this.ladderTextDescription.put(cell, ladderNumber.description);
    }

    public void setDeskPosition(final DecimalFormat formatter, final long position) {

        this.deskPosition.updateValue(formatter, position);

    }

    public void onPKSExposure(final DecimalFormat formatter, final PKSExposure data) {

        this.pksData = data;
        final double combinedPosition = data.getCombinedPosition();

        this.pksExposure.updateValue(formatter, data.dryExposure);
        this.pksPosition.updateValue(formatter, combinedPosition);
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