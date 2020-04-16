package com.drwtrading.london.reddal.util;

import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TinyStringDb {

    public static final String VALUE_KEY = "value";
    private final TinyJsonDb db;
    public final Map<String, String> cache = new HashMap<>();

    public TinyStringDb(final Path dataFile) throws IOException {

        this.db = new TinyJsonDb(dataFile);

        for (final String key : db.keys()) {
            get(key);
        }
    }

    public void put(final String id, final String value) {

        final String replace = value.replace("\n", "");
        db.put(id, new JSONObject(ImmutableMap.of(VALUE_KEY, replace)));
        cache.put(id, replace);
    }

    public String get(final String id) {

        if (cache.containsKey(id)) {
            return cache.get(id);
        } else {
            final JSONObject jsonObject = db.get(id);
            final String string = getString(jsonObject);
            cache.put(id, string);
            return string;
        }
    }

    public Set<Map.Entry<String, String>> entries() {
        return new HashSet<>(
                Collections2.transform(db.entries(), from -> new AbstractMap.SimpleImmutableEntry<>(from.getKey(), get(from.getKey()))));
    }

    private static String getString(final JSONObject jsonObject) {

        if (jsonObject == null || !jsonObject.has(VALUE_KEY)) {
            return null;
        } else {
            try {
                return jsonObject.getString(VALUE_KEY);
            } catch (final JSONException e) {
                System.out.println(jsonObject);
                throw new RuntimeException(e);
            }
        }
    }
}
