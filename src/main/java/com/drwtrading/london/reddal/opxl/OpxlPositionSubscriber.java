package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.reader.AOpxlReader;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.reddal.OPXLComponents;
import com.google.common.base.Strings;
import org.jetlang.channels.Publisher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class OpxlPositionSubscriber extends AOpxlReader<OPXLComponents, OPXLDeskPositions> {

    private final IFuseBox<OPXLComponents> monitor;
    private final Publisher<OPXLDeskPositions> positionPublisher;

    public OpxlPositionSubscriber(final SelectIO selectIO, final IFuseBox<OPXLComponents> monitor, final Collection<String> topics,
            final Publisher<OPXLDeskPositions> positionPublisher) {

        super(selectIO, selectIO, monitor, OPXLComponents.OPXL_POSITION_SUBSCRIBER, topics, "OPXLPositionSubscriber");
        this.monitor = monitor;
        this.positionPublisher = positionPublisher;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected OPXLDeskPositions parseTable(final Object[][] opxlTable) {

        final Map<String, Long> positions = new HashMap<>(opxlTable.length);
        final List<Object[]> failedRows = new LinkedList<>();

        for (int i = 1; i < opxlTable.length; ++i) {

            final Object[] data = opxlTable[i];
            try {

                final String symbol = data[0].toString();
                final String position = data[1].toString();

                if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                    final long pos = Long.parseLong(position);
                    positions.put(symbol, pos);
                }
            } catch (final Throwable throwable) {
                failedRows.add(data);
            }
        }

        if (failedRows.isEmpty()) {
            monitor.setOK(OPXLComponents.OPXL_POSITION_SUBSCRIBER_CLEAN_DATA);
        } else {
            final StringBuilder sb = new StringBuilder("Failed to load:\n");
            for (final Object[] data : failedRows) {

                sb.append(Arrays.asList(data));
                sb.append('\n');
            }
            sb.setLength(sb.length() - 1);
            monitor.logError(OPXLComponents.OPXL_POSITION_SUBSCRIBER_CLEAN_DATA, sb.toString());
        }

        return new OPXLDeskPositions(positions);
    }

    @Override
    protected void handleUpdate(final OPXLDeskPositions prevValue, final OPXLDeskPositions values) {

        positionPublisher.publish(values);
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
