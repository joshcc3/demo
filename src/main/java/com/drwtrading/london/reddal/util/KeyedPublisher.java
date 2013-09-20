package com.drwtrading.london.reddal.util;

public interface KeyedPublisher<K,T> {
    public void publish(K key, T value);
}
