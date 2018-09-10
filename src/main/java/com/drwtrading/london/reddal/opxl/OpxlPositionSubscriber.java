package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.opxl.reader.AOpxlReader;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IResourceMonitor;
import com.drwtrading.london.reddal.ReddalComponents;
import com.google.common.base.Strings;
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class OpxlPositionSubscriber extends AOpxlReader<ReddalComponents, OPXLDeskPositions> {

    private final Publisher<OPXLDeskPositions> positionPublisher;

    public OpxlPositionSubscriber(final SelectIO opxlSelectIO, final SelectIO callbackSelectIO,
            final IResourceMonitor<ReddalComponents> monitor, final Collection<String> topics,
            final Publisher<OPXLDeskPositions> positionPublisher) {

        super(opxlSelectIO, callbackSelectIO, monitor, ReddalComponents.OPXL_POSITION_SUBSCRIBER, topics, "OPXLPositionSubscriber");
        this.positionPublisher = positionPublisher;
    }

    @Override
    protected boolean isConnectionWanted() {
        return true;
    }

    @Override
    protected OPXLDeskPositions parseTable(final Object[][] opxlTable) {

        final Map<String, Long> positions = new HashMap<>();

        for (final Object[] data : opxlTable) {

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
        }

        return new OPXLDeskPositions(positions);
    }

    @Override
    protected void handleUpdate(final OPXLDeskPositions prevValue, final OPXLDeskPositions values) {

        positionPublisher.publish(values);
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
