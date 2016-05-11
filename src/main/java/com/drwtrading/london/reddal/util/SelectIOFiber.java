package com.drwtrading.london.reddal.util;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IErrorLogger;
import org.jetlang.core.Disposable;
import org.jetlang.fibers.Fiber;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public class SelectIOFiber implements Fiber {

    private final SelectIO selectIO;
    private final IErrorLogger errorLog;
    private final String threadName;

    private final Set<Disposable> disposables;

    public SelectIOFiber(final SelectIO selectIO, final IErrorLogger errorLog, final String threadName) {
        this.selectIO = selectIO;
        this.errorLog = errorLog;
        this.threadName = threadName;
        this.disposables = Collections.synchronizedSet(new HashSet<>());
    }

    @Override
    public void start() {
        try {
            selectIO.start(threadName);
        } catch (final IOException e) {
            errorLog.error("Error starting SelectIO [" + threadName + ']', e);
        }
    }

    @Override
    public void execute(final Runnable command) {
        final Runnable wrappedCmd = () -> {
            try {
                command.run();
            } catch (final Exception e) {
                errorLog.error("Failed to run command on SelectIO thread.", e);
            }
        };
        selectIO.execute(wrappedCmd);
    }

    @Override
    public Disposable schedule(final Runnable command, final long delay, final TimeUnit unit) {

        return scheduleAtFixedRate(command, delay, -1, unit);
    }

    @Override
    public Disposable scheduleWithFixedDelay(final Runnable command, final long initialDelay, final long delay, final TimeUnit unit) {

        return scheduleAtFixedRate(command, initialDelay, delay, unit);
    }

    public Disposable scheduleAtFixedRate(final Runnable command, final long initialDelay, final long period, final TimeUnit unit) {

        final long initDelayMillis = TimeUnit.MILLISECONDS.convert(initialDelay, unit);
        final long periodMillis = TimeUnit.MILLISECONDS.convert(period, unit);

        final SelectIODisposable disposable = new SelectIODisposable(command, periodMillis, errorLog);
        selectIO.addDelayedAction(initDelayMillis, disposable);
        return disposable;
    }

    @Override
    public void add(final Disposable disposable) {

        disposables.add(disposable);
    }

    @Override
    public boolean remove(final Disposable disposable) {

        return disposables.remove(disposable);
    }

    @Override
    public int size() {
        return disposables.size();
    }

    @Override
    public void dispose() {

        for (final Disposable disposable : disposables) {
            try {
                disposable.dispose();
            } catch (final Exception e) {
                errorLog.error("Failed to dispose of disposable.", e);
            }
        }
    }
}
