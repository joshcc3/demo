package com.drwtrading.london.reddal.opxl;

import com.google.common.base.Strings;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;

public class OpxlPositionSubscriber {

    private final Publisher<Throwable> errorPublisher;
    private final Publisher<OPXLDeskPositions> positionPublisher;

    public OpxlPositionSubscriber(final Publisher<Throwable> errorPublisher, final Publisher<OPXLDeskPositions> positionPublisher) {

        this.errorPublisher = errorPublisher;
        this.positionPublisher = positionPublisher;
    }

    public void onOpxlData(final OpxlData opxlData) {

        try {

            final Map<String, Long> positions = new HashMap<>();

            for (final Object[] data : opxlData.getData()) {

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

            positionPublisher.publish(new OPXLDeskPositions(positions));

        } catch (final Throwable throwable) {
            errorPublisher.publish(throwable);
        }
    }

}
