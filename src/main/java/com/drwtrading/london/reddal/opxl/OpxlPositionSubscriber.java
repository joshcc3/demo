package com.drwtrading.london.reddal.opxl;

import com.drwtrading.photons.ladder.DeskPosition;
import com.google.common.base.Strings;
import drw.opxl.OpxlCallbacks;
import drw.opxl.OpxlClient;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;

import java.io.IOException;

import static com.google.common.collect.Sets.newHashSet;

public class OpxlPositionSubscriber {

    private final Publisher<Throwable> errorPublisher;
    private final Publisher<DeskPosition> positionPublisher;

    public OpxlPositionSubscriber(Publisher<Throwable> errorPublisher, Publisher<DeskPosition> positionPublisher) {
        this.errorPublisher = errorPublisher;
        this.positionPublisher = positionPublisher;
    }

    public void onOpxlData(OpxlData opxlData) {
        try {
            for (Object[] data : opxlData.getData()) {
                if (data.length == 2) {
                    String symbol = data[0].toString();
                    String position = data[1].toString();
                    if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                        positionPublisher.publish(new DeskPosition(symbol, position));
                    }
                } else if (data.length == 3) {
                    String symbol = data[1].toString();
                    String position = data[2].toString();
                    if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                        positionPublisher.publish(new DeskPosition(symbol.toUpperCase(), position));
                    }
                }

            }
        } catch (Throwable throwable) {
            errorPublisher.publish(throwable);
        }
    }

}
