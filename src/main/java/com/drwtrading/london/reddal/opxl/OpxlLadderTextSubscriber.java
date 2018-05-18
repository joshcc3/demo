package com.drwtrading.london.reddal.opxl;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.reddal.data.LaserLineType;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.util.DoOnce;
import com.drwtrading.photons.ladder.LadderMetadata;
import com.drwtrading.photons.ladder.LadderText;
import com.google.common.base.Strings;
import com.google.common.collect.Sets;
import drw.opxl.OpxlData;
import org.jetlang.channels.Publisher;

import java.util.Set;

public class OpxlLadderTextSubscriber {

    private static final int MAX_LENGTH_OF_TEXT = 6;

    private final Publisher<Throwable> errorPublisher;
    private final Publisher<LaserLineValue> laserLinePublisher;
    private final Publisher<LadderMetadata> metaPublisher;

    private final DoOnce latch = new DoOnce();

    private final Set<String> validCells = Sets.newHashSet("r1c1", "r1c2", "r1c3", "r1c4", "r2c1", "r2c3", "r3c2", "r3c3", "r3c4");

    public OpxlLadderTextSubscriber(final Publisher<Throwable> errorPublisher, final Publisher<LaserLineValue> laserLinePublisher,
            final Publisher<LadderMetadata> metaPublisher) {

        this.errorPublisher = errorPublisher;
        this.laserLinePublisher = laserLinePublisher;
        this.metaPublisher = metaPublisher;
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

                        final LaserLineType laserType;
                        if ("bid".equals(color)) {
                            laserType = LaserLineType.BID;
                        } else if ("offer".equals(color)) {
                            laserType = LaserLineType.ASK;
                        } else {
                            laserType = LaserLineType.GREEN;
                        }

                        final String trimmedValue = value.trim();
                        if (trimmedValue.isEmpty() || "#ERR".equals(trimmedValue)) {
                            laserLinePublisher.publish(new LaserLineValue(symbol, laserType));
                        } else {
                            try {
                                final long price = (long) (Constants.NORMALISING_FACTOR * Double.parseDouble(trimmedValue));
                                laserLinePublisher.publish(new LaserLineValue(symbol, laserType, price));
                            } catch (final NumberFormatException e) {
                                laserLinePublisher.publish(new LaserLineValue(symbol, laserType));
                                latch.doOnce(() -> {
                                    errorPublisher.publish(
                                            new RuntimeException("Could not format: " + trimmedValue + " for " + symbol + ' ' + cell));
                                    e.printStackTrace();
                                });
                            }
                        }
                    } else if (!validCells.contains(cell)) {
                        latch.doOnce(() -> errorPublisher.publish(new Throwable("Opxl Ladder Text Cell is not valid, got:" + cell)));
                    } else {
                        final LadderText text =
                                new LadderText(symbol, cell, value.substring(0, Math.min(value.length(), MAX_LENGTH_OF_TEXT)), color);
                        metaPublisher.publish(text);
                        latch.reset();
                    }
                }
            }
        } catch (final Throwable throwable) {
            latch.doOnce(() -> errorPublisher.publish(throwable));
        }
    }
}
