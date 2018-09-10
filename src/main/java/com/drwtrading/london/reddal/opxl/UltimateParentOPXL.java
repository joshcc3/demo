package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.reader.AOpxlLoggingReader;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import org.jetlang.channels.Publisher;

import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class UltimateParentOPXL extends AOpxlLoggingReader<ReddalComponents, Collection<UltimateParentMapping>> {

    private static final String TOPIC = "eeif.parent.original";

    private static final String PARENT_COL = "parent";
    private static final String CHILD_COL = "original";
    private static final String RATIO_COL = "1_child_per_x_parents";

    private final Publisher<UltimateParentMapping> ultimateParentPublisher;

    public UltimateParentOPXL(final SelectIO opxlSelectIO, final SelectIO callbackSelectIO,
            final IResourceMonitor<ReddalComponents> monitor, final Publisher<UltimateParentMapping> ultimateParentPublisher,
            final Path logPath) {

        super(opxlSelectIO, callbackSelectIO, monitor, ReddalComponents.OPXL_ULTIMATE_PARENT, TOPIC, logPath);

        this.ultimateParentPublisher = ultimateParentPublisher;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected Collection<UltimateParentMapping> parseTable(final Object[][] opxlTable) {

        if (0 < opxlTable.length) {

            final Object[] headerRow = opxlTable[0];

            final int childCol = findColumn(headerRow, CHILD_COL);
            final int parentCol = findColumn(headerRow, PARENT_COL);
            final int ratioCol = findColumn(headerRow, RATIO_COL);

            final Map<String, UltimateParentMapping> isinToParent = new HashMap<>();

            boolean allOK = true;

            for (int i = 1; i < opxlTable.length; ++i) {

                final Object[] row = opxlTable[i];

                if (testColsPresent(row, childCol, parentCol)) {

                    final String child = row[childCol].toString();
                    final String parent = row[parentCol].toString();
                    final String optionalRatio = row[ratioCol].toString();

                    try {
                        final InstrumentID childInstID = InstrumentID.getFromIsinCcyMic(child);
                        final InstrumentID parentInstID = InstrumentID.getFromIsinCcyMic(parent);
                        final double childPerParentRatio = optionalRatio.isEmpty() ? 1d : Double.parseDouble(optionalRatio);

                        final UltimateParentMapping ultimateParentMapping =
                                new UltimateParentMapping(childInstID.isin, parentInstID, 1 / childPerParentRatio);

                        final UltimateParentMapping prevUltimateParent = isinToParent.putIfAbsent(childInstID.isin, ultimateParentMapping);
                        if (null != prevUltimateParent && !parentInstID.equals(prevUltimateParent.parentID)) {

                            logErrorOnSelectIO(
                                    "Ultimate parent differs for [" + childInstID.isin + "], first [" + prevUltimateParent.parentID +
                                            "] and then [" + ultimateParentMapping.parentID + "].");
                            allOK = false;
                        }
                    } catch (final Exception e) {
                        logErrorOnSelectIO("Could not parse ultimate parent line " + Arrays.toString(row) + " in ultimate parent file.", e);
                    }
                }
            }
            if (allOK) {
                return isinToParent.values();
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    protected void handleUpdate(final Collection<UltimateParentMapping> prevValue, final Collection<UltimateParentMapping> values) {

        for (final UltimateParentMapping ultimateParent : values) {
            ultimateParentPublisher.publish(ultimateParent);
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
