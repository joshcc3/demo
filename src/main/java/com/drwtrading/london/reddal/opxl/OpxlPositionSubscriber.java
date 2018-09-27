package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.reader.AOpxlReader;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.OPXLComponents;
import com.google.common.base.Strings;
import org.jetlang.channels.Publisher;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OpxlPositionSubscriber extends AOpxlReader<OPXLComponents, OPXLDeskPositions> {

    private final IResourceMonitor<OPXLComponents> monitor;
    private final Publisher<OPXLDeskPositions> positionPublisher;

    public OpxlPositionSubscriber(final SelectIO selectIO, final IResourceMonitor<OPXLComponents> monitor, final Collection<String> topics,
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
        for (final Object[] data : opxlTable) {

            try {
                if (2 == data.length) {

                    final String symbol = data[0].toString();
                    final String position = data[1].toString();

                    if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                        final long pos = Long.parseLong(position);
                        positions.put(symbol, pos);
                    }
                } else if (3 == data.length) {

                    final String symbol = data[1].toString();
                    final String position = data[2].toString();

                    if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                        final long pos = Long.parseLong(position);
                        positions.put(symbol.toUpperCase(), pos);
                    }
                }
            } catch (Throwable throwable) {
                monitor.logError(OPXLComponents.OPXL_POSITION_SUBSCRIBER, "Failed to load: " + Arrays.asList(data), throwable);
            }
        }

        monitor.setOK(OPXLComponents.OPXL_POSITION_SUBSCRIBER);
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
