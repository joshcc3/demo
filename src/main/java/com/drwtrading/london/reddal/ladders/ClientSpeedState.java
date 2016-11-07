package com.drwtrading.london.reddal.ladders;

public enum ClientSpeedState {
    TOO_SLOW(10000),
    SLOW(5000),
    FINE(0);

    public final int thresholdMillis;

    ClientSpeedState(final int thresholdMillis) {
        this.thresholdMillis = thresholdMillis;
    }
}
