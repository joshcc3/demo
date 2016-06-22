package com.drwtrading.london.reddal.util;

import org.joda.time.DateTime;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public interface TinyDb<T> {

    public DateTime lastUpdated();

    public void put(final String id, final T value);

    public T get(final String id);

    public Collection<T> getAll(final String id);

    public Collection<Map.Entry<String, T>> entries();

    public Set<String> keys();
}
