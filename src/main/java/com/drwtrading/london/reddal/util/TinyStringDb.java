package com.drwtrading.london.reddal.util;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TinyStringDb implements TinyDb<String> {

    public static final String VALUE_KEY = "value";
    private final TinyJsonDb db;
    public final Map<String, String> cache = new HashMap<>();

    public TinyStringDb(final Path dataFile) throws IOException {
        db = new TinyJsonDb(dataFile);
        // Pre-cache
        for (final String key : db.keys()) {
            get(key);
        }
    }

    @Override
    public DateTime lastUpdated() {
        return db.lastUpdated();
    }

    @Override
    public void put(final String id, final String value) {
        final String replace = value.replace("\n", "");
        db.put(id, new JSONObject(ImmutableMap.of(VALUE_KEY, replace)));
        cache.put(id, replace);
    }

    @Override
    public String get(final String id) {
        if (cache.containsKey(id)) {
            return cache.get(id);
        }
        final JSONObject jsonObject = db.get(id);
        final String string = getString(jsonObject);
        cache.put(id, string);
        return string;
    }

    @Override
    public Collection<String> getAll(final String id) {
        return Collections2.transform(db.getAll(id), new Function<JSONObject, String>() {
            @Override
            public String apply(final org.json.JSONObject from) {
                return getString(from);
            }
        });
    }

    @Override
    public Set<Map.Entry<String, String>> entries() {
        return new HashSet<>(Collections2.transform(db.entries(), new Function<Map.Entry<String, JSONObject>, Map.Entry<String, String>>() {
            @Override
            public Map.Entry<String, String> apply(final Map.Entry<String, JSONObject> from) {
                return new AbstractMap.SimpleImmutableEntry<>(from.getKey(), get(from.getKey()));
            }
        }));
    }

    @Override
    public Set<String> keys() {
        return db.keys();
    }

    private static String getString(final JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            return jsonObject.getString(VALUE_KEY);
        } catch (final JSONException e) {
            System.out.println(jsonObject);
            throw new RuntimeException(e);
        }
    }
}
