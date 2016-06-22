package com.drwtrading.london.reddal.opxl;

import com.drwtrading.photons.ladder.DeskPosition;
import com.google.common.base.Strings;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;

public class OpxlPositionSubscriber {

    private final Publisher<Throwable> errorPublisher;
    private final Publisher<DeskPosition> positionPublisher;

    public OpxlPositionSubscriber(final Publisher<Throwable> errorPublisher, final Publisher<DeskPosition> positionPublisher) {
        this.errorPublisher = errorPublisher;
        this.positionPublisher = positionPublisher;
    }

    public void onOpxlData(final OpxlData opxlData) {
        try {
            for (final Object[] data : opxlData.getData()) {
                if (data.length == 2) {
                    final String symbol = data[0].toString();
                    final String position = data[1].toString();
                    if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                        positionPublisher.publish(new DeskPosition(symbol, position));
                    }
                } else if (data.length == 3) {
                    final String symbol = data[1].toString();
                    final String position = data[2].toString();
                    if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                        positionPublisher.publish(new DeskPosition(symbol.toUpperCase(), position));
                    }
                }

            }
        } catch (final Throwable throwable) {
            errorPublisher.publish(throwable);
        }
    }

}
