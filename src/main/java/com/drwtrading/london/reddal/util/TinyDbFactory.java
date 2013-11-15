package com.drwtrading.london.reddal.util;
public interface TinyDbFactory {
    public <T> TinyDb<T> getDb(Class<T> clazz);
}
