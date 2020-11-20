package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.io.SelectIO;
import org.jetlang.core.Callback;

import java.util.concurrent.CopyOnWriteArrayList;

public class SelectIOChannel<T> {

    private final CopyOnWriteArrayList<Callback<T>> subscribers;

    public SelectIOChannel() {
        this.subscribers = new CopyOnWriteArrayList<>();
    }

    public void subscribe(final Callback<T> callback) {

        subscribers.addIfAbsent(callback);
    }

    public void subscribe(final SelectIO selectIO, final Callback<T> callback) {

        final Callback<T> laterCallback = message -> selectIO.execute(() -> callback.onMessage(message));
        subscribers.addIfAbsent(laterCallback);
    }

    public void publish(final T object) {

        subscribers.forEach(s -> s.onMessage(object));
    }
}
