package com.drwtrading.london.reddal.util;

import com.drwtrading.london.time.Clock;

public class DoOnceEveryXMillis {

    private boolean toDo = true;
    private final Clock clock;
    private final int millis;
    private long lastTimeDid = 0;

    public DoOnceEveryXMillis(final Clock clock, final int millis) {
        this.clock = clock;
        this.millis = millis;
    }

    public void doItEveryXMillis(final Runnable runnable) {
        if (toDo || (lastTimeDid + millis < clock.now())) {
            toDo = false;
            lastTimeDid = clock.now();
            runnable.run();
        }
    }
}
