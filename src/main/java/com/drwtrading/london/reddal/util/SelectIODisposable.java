package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.utils.io.ISelectIORunnable;
import com.drwtrading.london.eeif.utils.monitoring.IErrorLogger;
import org.jetlang.core.Disposable;

class SelectIODisposable implements ISelectIORunnable, Disposable {

    private final Runnable command;
    private final long periodMillis;
    private final IErrorLogger errorLog;

    private volatile boolean active;

    SelectIODisposable(final Runnable command, final long periodMillis, final IErrorLogger errorLog) {

        this.command = command;
        this.periodMillis = periodMillis;
        this.errorLog = errorLog;

        this.active = true;
    }

    @Override
    public void dispose() {
        this.active = false;
    }

    @Override
    public long run() {

        if (active) {
            try {
                command.run();
            } catch (final Exception e) {
                errorLog.error("Failed to run command on SelectIO thread.", e);
            }
            return periodMillis;
        } else {
            return -1;
        }
    }
}
