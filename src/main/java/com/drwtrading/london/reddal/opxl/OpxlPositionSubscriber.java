package com.drwtrading.london.reddal.opxl;

import com.drwtrading.photons.ladder.DeskPosition;
import com.google.common.base.Strings;
import drw.opxl.OpxlCallbacks;
import drw.opxl.OpxlClient;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;

import static com.google.common.collect.Sets.newHashSet;

public class OpxlPositionSubscriber {
    private final Publisher<Throwable> errorPublisher;
    private final Publisher<DeskPosition> positionPublisher;
    private final String key;
    private OpxlClient opxlClient;


    public OpxlPositionSubscriber(String opxlHost, int opxlPort, Publisher<Throwable> errorPublisher, String key, Publisher<DeskPosition> positionPublisher) {
        this.errorPublisher = errorPublisher;
        this.positionPublisher = positionPublisher;
        this.key = key;
        this.opxlClient = new OpxlClient(opxlHost, opxlPort, newHashSet(key), onData());
    }

    private OpxlCallbacks onData() {
        return new OpxlCallbacks() {
            @Override
            public void on(OpxlData opxlData) {
                try {
                    for (Object[] data : opxlData.getData()) {
                        if(data.length == 2) {
                            String symbol = data[0].toString();
                            String position = data[1].toString();
                            if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                                positionPublisher.publish(new DeskPosition(symbol, position));
                            }
                        } else if (data.length == 3) {
                            String symbol = data[1].toString();
                            String position = data[2].toString();
                            if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(position)) {
                                positionPublisher.publish(new DeskPosition(symbol, position));
                            }
                        }

                    }
                } catch (Throwable throwable) {
                    errorPublisher.publish(throwable);
                }
            }

            @Override
            public void onDisconnect() {
                errorPublisher.publish(new Throwable("Opxl " + this.getClass().getSimpleName() + " Subscriber disconnected"));
            }
        };
    }

    public Runnable connectToOpxl() {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    opxlClient.connect();
                } catch (Exception e) {
                    final String message = "Opxl " + this.getClass().getSimpleName() + " Subscriber connection failure:";
                    errorPublisher.publish(new Throwable(message, e));
                }
            }
        };
    }
}
