package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.reader.AOpxlReader;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.data.LaserLineType;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LadderText;
import com.google.common.collect.Sets;
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Set;

public class OpxlLadderTextSubscriber extends AOpxlReader<ReddalComponents, Collection<OpxlLadderTextRow>> {

    private static final Set<String> VALID_CELLS = Sets.newHashSet("r1c1", "r1c2", "r1c3", "r1c4", "r2c1", "r2c3", "r3c2", "r3c3", "r3c4");

    private static final int SYMBOL_COL = 0;
    private static final int CELL_COL = 1;
    private static final int VALUE_COL = 2;
    private static final int COLOUR_COL = 3;

    private static final int MAX_LENGTH_OF_TEXT = 6;

    private final Publisher<LaserLineValue> laserLinePublisher;
    private final Publisher<LadderMetadata> metaPublisher;

    public OpxlLadderTextSubscriber(final SelectIO opxlSelectIO, final SelectIO callbackSelectIO,
            final IResourceMonitor<ReddalComponents> monitor, final Collection<String> topic,
            final Publisher<LaserLineValue> laserLinePublisher, final Publisher<LadderMetadata> metaPublisher) {

        super(opxlSelectIO, callbackSelectIO, monitor, ReddalComponents.OPXL_LADDER_TEXT, topic, "OpxlLadderTextSubscriber");

        this.laserLinePublisher = laserLinePublisher;
        this.metaPublisher = metaPublisher;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected Collection<OpxlLadderTextRow> parseTable(final Object[][] opxlTable) {

        final Collection<OpxlLadderTextRow> rows = new LinkedList<>();

        for (final Object[] data : opxlTable) {

            if (testColsPresent(data, SYMBOL_COL, CELL_COL, VALUE_COL, COLOUR_COL)) {

                final String symbol = data[SYMBOL_COL].toString();
                final String cell = data[CELL_COL].toString();
                final String value = data[VALUE_COL].toString();
                final String colour = data[COLOUR_COL].toString();

                final OpxlLadderTextRow row = new OpxlLadderTextRow(symbol, cell, value, colour);
                rows.add(row);
            }
        }
        return rows;
    }

    @Override
    protected void handleUpdate(final Collection<OpxlLadderTextRow> prevValue, final Collection<OpxlLadderTextRow> values) {

        for (final OpxlLadderTextRow row : values) {

            if (row.cell.startsWith("laser")) {

                final LaserLineType laserType;
                if ("bid".equals(row.colour)) {
                    laserType = LaserLineType.BID;
                } else if ("offer".equals(row.colour)) {
                    laserType = LaserLineType.ASK;
                } else {
                    laserType = LaserLineType.GREEN;
                }

                final String trimmedValue = row.value.trim();
                if (trimmedValue.isEmpty() || "#ERR".equals(trimmedValue)) {
                    laserLinePublisher.publish(new LaserLineValue(row.symbol, laserType));
                } else {
                    try {
                        final long price = (long) (Constants.NORMALISING_FACTOR * Double.parseDouble(trimmedValue));
                        laserLinePublisher.publish(new LaserLineValue(row.symbol, laserType, price));
                    } catch (final NumberFormatException e) {
                        laserLinePublisher.publish(new LaserLineValue(row.symbol, laserType));
                        monitor.logError(component, "Could not format: " + trimmedValue + " for " + row.symbol + ' ' + row.cell, e);
                    }
                }
            } else if (VALID_CELLS.contains(row.cell)) {
                final String text = row.value.substring(0, Math.min(row.value.length(), MAX_LENGTH_OF_TEXT));
                final LadderText ladderText = new LadderText(row.symbol, row.cell, text, row.colour);
                metaPublisher.publish(ladderText);
            } else {
                monitor.logError(component, "Opxl Ladder Text Cell is not valid, got:" + row.cell);
            }
        }
    }

    @Override
    protected void handleError(final ReddalComponents component, final String msg) {
        monitor.logError(component, msg);
    }

    @Override
    protected void handleError(final ReddalComponents component, final String msg, final Throwable t) {
        monitor.logError(component, msg, t);
    }
}
