package com.drwtrading.london.reddal.util;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.ImmutableMap;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class TinyStringDb implements TinyDb<String>{

    public static final String VALUE_KEY = "value";
    private final TinyJsonDb db;
    public final Map<String, String> cache = new HashMap<String, String>();

    public TinyStringDb(File dataFile) throws IOException {
        db = new TinyJsonDb(dataFile);
        // Pre-cache
        for (String key : db.keys()) {
            get(key);
        }
    }

    @Override
    public DateTime lastUpdated() {
        return db.lastUpdated();
    }

    public void put(String id, String value) {
        String replace = value.replace("\n", "");
        db.put(id, new JSONObject(ImmutableMap.of(VALUE_KEY, replace)));
        cache.put(id, replace);
    }

    public String get(String id) {
        if(cache.containsKey(id)) {
            return cache.get(id);
        }
        JSONObject jsonObject = db.get(id);
        String string = getString(jsonObject);
        cache.put(id, string);
        return string;
    }

    public Collection<String> getAll(String id) {
        return Collections2.transform(db.getAll(id), new Function<JSONObject, String>() {
            @Override
            public String apply(org.json.JSONObject from) {
                return getString(from);
            }
        });
    }

    public Set<Map.Entry<String, String>> entries() {
        return new HashSet<Map.Entry<String, String>>(Collections2.transform(db.entries(), new Function<Map.Entry<String, JSONObject>, Map.Entry<String, String>>() {
            @Override
            public Map.Entry<String, String> apply(Map.Entry<String, JSONObject> from) {
                return new AbstractMap.SimpleImmutableEntry<String, String>(from.getKey(), get(from.getKey()));
            }
        }));
    }

    @Override
    public Set<String> keys() {
        return db.keys();
    }

    private String getString(JSONObject jsonObject) {
        if (jsonObject == null) {
            return null;
        }
        try {
            return jsonObject.getString(VALUE_KEY);
        } catch (JSONException e) {
            System.out.println(jsonObject.toString());
            throw new RuntimeException(e);
        }
    }
}
