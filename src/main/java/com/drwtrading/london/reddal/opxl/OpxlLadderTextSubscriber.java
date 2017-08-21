package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.reddal.util.DoOnce;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LadderText;
import com.drwtrading.photons.ladder.LaserLine;
import com.google.common.base.Strings;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;

import java.math.BigDecimal;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;

public class OpxlLadderTextSubscriber {

    public static final int MAX_LENGTH_OF_TEXT = 6;
    private final Publisher<Throwable> errorPublisher;
    private final Publisher<LadderMetadata> publisher;
    public final DoOnce latch = new DoOnce();

    private final Set<String> validCells =
            newHashSet("r1c1", "r1c2", "r1c3", "r1c4", "r2c1", "r2c2", "r2c3", "r2c4", "r3c2", "r3c3", "r3c4");

    public OpxlLadderTextSubscriber(final Publisher<Throwable> errorPublisher, final Publisher<LadderMetadata> publisher) {
        this.errorPublisher = errorPublisher;
        this.publisher = publisher;
    }

    public void onOpxlData(final OpxlData opxlData) {
        try {
            for (final Object[] data : opxlData.getData()) {
                final String symbol = data[0].toString();
                final String cell = data[1].toString();
                final String value = data[2].toString();
                final String color = data[3].toString();
                if (!Strings.isNullOrEmpty(symbol) && !Strings.isNullOrEmpty(cell)) {
                    if (cell.startsWith("laser")) {

                        final String laserColor;
                        if ("bid".equals(color)) {
                            laserColor = "bid";
                        } else if ("offer".equals(color)) {
                            laserColor = "offer";
                        } else {
                            laserColor = "GREEN";
                        }

                        if (value.trim().isEmpty() || "#ERR".equals(value.trim())) {
                            publisher.publish(new LaserLine(symbol, laserColor, 0, false, "EEIF"));
                        } else {
                            try {
                                publisher.publish(
                                        new LaserLine(symbol, laserColor, new BigDecimal(value).movePointRight(9).longValue(), true,
                                                "EEIF"));
                            } catch (final NumberFormatException e) {
                                publisher.publish(new LaserLine(symbol, "laserColor", 0, false, "EEIF"));
                                latch.doOnce(() -> {
                                    errorPublisher.publish(
                                            new RuntimeException("Could not format: " + value + " for " + symbol + ' ' + cell));
                                    e.printStackTrace();
                                });
                            }
                        }
                    } else if (!validCells.contains(cell)) {
                        latch.doOnce(() -> errorPublisher.publish(new Throwable("Opxl Ladder Text Cell is not valid, got:" + cell)));
                    } else {
                        publisher.publish(
                                new LadderText(symbol, cell, value.substring(0, Math.min(value.length(), MAX_LENGTH_OF_TEXT)), color));
                        latch.reset();
                    }
                }
            }
        } catch (final Throwable throwable) {
            latch.doOnce(() -> errorPublisher.publish(throwable));
        }
    }
}
