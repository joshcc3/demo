package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.reader.AOpxlReader;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.icepie.transport.data.LaserLineType;
import com.drwtrading.london.reddal.OPXLComponents;
import com.drwtrading.london.reddal.data.LaserLine;
import com.drwtrading.london.reddal.fastui.html.ReddalFreeTextCell;
import org.jetlang.channels.Publisher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class OpxlLadderTextSubscriber extends AOpxlReader<OPXLComponents, OpxlLadderTextRow> {

    private static final int SYMBOL_COL = 0;
    private static final int CELL_COL = 1;
    private static final int VALUE_COL = 2;
    private static final int COLOUR_COL = 3;
    private static final int DESCRIPTION_COL = 4;

    private static final int MAX_LENGTH_OF_TEXT = 6;

    private final Publisher<LaserLine> laserLinePublisher;
    private final Publisher<LadderTextUpdate> ladderTextPublisher;

    public OpxlLadderTextSubscriber(final SelectIO selectIO, final IFuseBox<OPXLComponents> monitor, final Collection<String> topics,
            final Publisher<LaserLine> laserLinePublisher, final Publisher<LadderTextUpdate> ladderTextPublisher) {

        super(selectIO, selectIO, monitor, OPXLComponents.OPXL_LADDER_TEXT, topics, "OpxlLadderTextSubscriber");

        this.laserLinePublisher = laserLinePublisher;
        this.ladderTextPublisher = ladderTextPublisher;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected OpxlLadderTextRow parseTable(final Object[][] opxlTable) {

        final List<LadderTextUpdate> ladderTexts = new ArrayList<>(opxlTable.length);
        final List<LaserLine> laserLines = new ArrayList<>(opxlTable.length);

        for (final Object[] data : opxlTable) {

            if (testColsPresent(data, SYMBOL_COL, CELL_COL, VALUE_COL)) {

                final String symbol = data[SYMBOL_COL].toString();
                final String cell = data[CELL_COL].toString();
                final String value = data[VALUE_COL].toString().trim();
                final String description = data[DESCRIPTION_COL].toString().trim();

                if (cell.startsWith("laser")) {

                    final String colour = data[COLOUR_COL].toString();
                    final LaserLine laserLineValue = getLaserLine(symbol, cell, value, colour);
                    laserLines.add(laserLineValue);

                } else {

                    final ReddalFreeTextCell freeTextCell = ReddalFreeTextCell.getCell(cell);
                    if (null == freeTextCell) {
                        logErrorOnSelectIO("Opxl Ladder Text Cell is not valid [" + cell + "] for [" + symbol + "].");
                    } else {

                        final String text;
                        if (MAX_LENGTH_OF_TEXT < value.length()) {
                            text = value.substring(0, Math.min(value.length(), MAX_LENGTH_OF_TEXT));
                        } else {
                            text = value;
                        }

                        final LadderTextUpdate ladderText = new LadderTextUpdate(symbol, freeTextCell, text, description);
                        ladderTexts.add(ladderText);
                    }
                }
            }
        }

        return new OpxlLadderTextRow(ladderTexts, laserLines);
    }

    private LaserLine getLaserLine(final String symbol, final String cell, final String value, final String colour) {

        final LaserLineType laserType;
        if ("bid".equals(colour)) {
            laserType = LaserLineType.BID;
        } else if ("offer".equals(colour)) {
            laserType = LaserLineType.ASK;
        } else {
            laserType = LaserLineType.GREEN;
        }

        if (value.isEmpty() || "#ERR".equals(value)) {
            return new LaserLine(symbol, laserType);
        } else {
            try {
                final long price = (long) (Constants.NORMALISING_FACTOR * Double.parseDouble(value));
                return new LaserLine(symbol, laserType, price);
            } catch (final NumberFormatException e) {
                logErrorOnSelectIO("Could not format: " + value + " for " + symbol + ' ' + cell, e);
                return new LaserLine(symbol, laserType);
            }
        }
    }

    @Override
    protected void handleUpdate(final OpxlLadderTextRow prevValue, final OpxlLadderTextRow values) {

        for (final LaserLine laserLineValue : values.laserLines) {
            laserLinePublisher.publish(laserLineValue);
        }

        for (final LadderTextUpdate ladderText : values.ladderText) {
            ladderTextPublisher.publish(ladderText);
        }
    }

    @Override
    protected void handleError(final OPXLComponents component, final String msg) {
        monitor.logError(component, msg);
    }

    @Override
    protected void handleError(final OPXLComponents component, final String msg, final Throwable t) {
        monitor.logError(component, msg, t);
    }
}
