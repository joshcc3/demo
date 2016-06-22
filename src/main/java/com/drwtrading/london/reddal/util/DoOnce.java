package com.drwtrading.london.reddal.util;

public class DoOnce {

    private boolean toDo = true;

    public void doOnce(final Runnable runnable) {
        if (toDo) {
            toDo = false;
            runnable.run();
        }
    }

    public void reset() {
        toDo = true;
    }
}
