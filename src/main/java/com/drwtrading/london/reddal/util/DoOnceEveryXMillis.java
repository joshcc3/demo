package com.drwtrading.london.reddal.util;

import com.drwtrading.london.time.Clock;

public class DoOnceEveryXMillis {
    private boolean didOnce = false;
    private final Clock clock;
    private final int millis;
    private long lastTimeDid = 0;

    public DoOnceEveryXMillis(Clock clock, int millis) {
        this.clock = clock;
        this.millis = millis;
    }

    public void doItEveryXMillis(Runnable runnable){
        if (!didOnce || (lastTimeDid + millis < clock.now())){
            didOnce = true;
            lastTimeDid = clock.now();
            runnable.run();
        }
    }
    public void reset(){
        didOnce = false;
        lastTimeDid = 0;
    }
    public boolean calledAtLeastOnce(){
        return didOnce;
    }
}
