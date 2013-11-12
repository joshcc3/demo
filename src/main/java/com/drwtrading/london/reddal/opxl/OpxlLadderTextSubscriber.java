package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.reddal.util.DoOnce;
import com.drwtrading.photons.ladder.LadderText;
import com.google.common.base.Strings;
import drw.opxl.OpxlCallbacks;
import drw.opxl.OpxlClient;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;

import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class OpxlLadderTextSubscriber {
    private final Publisher<Throwable> errorPublisher;
    private final Publisher<LadderText> positionPublisher;
    private OpxlClient opxlClient;
    public final DoOnce latch = new DoOnce();

    private final Set<String> validCells = newHashSet(
            "r1c1",
            "r1c2",
            "r1c3",
            "r1c4",
            "r1c5",
            "r2c1",
            "r2c2",
            "r2c3",
            "r2c4",
            "r2c5",
            "r3c1",
            "r3c2",
            "r3c3",
            "r3c4",
            "r3c5");

    public OpxlLadderTextSubscriber(String opxlHost, int opxlPort, Publisher<Throwable> errorPublisher, String forexKey, Publisher<LadderText> positionPublisher) {
        this.errorPublisher = errorPublisher;
        this.positionPublisher = positionPublisher;
        this.opxlClient = new OpxlClient(opxlHost, opxlPort, newHashSet(forexKey), onData());
    }

    private OpxlCallbacks onData() {
        return new OpxlCallbacks() {
            @Override
            public void on(OpxlData opxlData) {
                try {
                    for (Object[] data : opxlData.getData()) {
                        String symbol = data[0].toString();
                        final String cell = data[1].toString();
                        String value = data[2].toString();
                        String color = data[3].toString();
                        if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(cell)) {
                            if (!validCells.contains(cell)) {
                                latch.doOnce(new Runnable() {
                                    @Override
                                    public void run() {
                                        errorPublisher.publish(new Throwable("Opxl Ladder Text Cell is not valid, got:" + cell));
                                    }
                                });
                            } else {
                                positionPublisher.publish(new LadderText(symbol, cell, value.substring(0, Math.min(value.length(), 4)), color));
                                latch.reset();
                            }
                        }
                    }
                } catch (final Throwable throwable) {
                    latch.doOnce(new Runnable() {
                        @Override
                        public void run() {
                            errorPublisher.publish(throwable);
                        }
                    });
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
