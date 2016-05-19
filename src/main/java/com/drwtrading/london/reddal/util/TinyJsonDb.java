package com.drwtrading.london.reddal.util;

import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.MapMaker;
import com.google.common.io.Files;
import drw.london.json.Jsonable;
import org.joda.time.DateTime;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TinyJsonDb implements TinyDb<JSONObject> {

    public static final String LAST_UPDATED = "__lastUpdated";

    public static class JsonFileDbEntry implements Jsonable {

        public final String id;
        public final Long time;
        public final JSONObject value;

        public JsonFileDbEntry(final String id, final Long time, final JSONObject data) {
            this.id = id;
            this.time = time;
            this.value = data;
        }

        public static JsonFileDbEntry fromJson(final JSONObject jsonObject) throws JSONException {
            return new JsonFileDbEntry(
                    jsonObject.getString("id"),
                    jsonObject.getLong("time"),
                    jsonObject.getJSONObject("value"));
        }

        @Override
        public void toJson(final Appendable out) throws IOException {
            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", id);
                jsonObject.put("time", time);
                jsonObject.put("value", value);
                out.append(jsonObject.toString());
            } catch (final JSONException e) {
                return;
            }
        }
    }

    public final File dataFile;
    public final Map<String, JsonFileDbEntry> objectById = new HashMap<>();
    public final PrintWriter printWriter;
    public boolean isLoaded = false;
    public boolean isClosed = false;

    public TinyJsonDb(final Path dataFile) throws IOException {
        this.dataFile = dataFile.toFile();
        printWriter = new PrintWriter(new FileWriter(this.dataFile, true));
        load();
    }


    public boolean isLoaded() {
        return isLoaded;
    }

    public void load() throws IOException {
        if (dataFile.exists()) {
            final List<String> strings = Files.readLines(dataFile, Charset.defaultCharset());
            for (final String string : strings) {
                try {
                    final JSONObject lineObj = new JSONObject(string);
                    final JsonFileDbEntry entry = JsonFileDbEntry.fromJson(lineObj);
                    store(entry);
                } catch (final JSONException e) {
                    System.out.println("Error parsing line: " + string);
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        isLoaded = true;
    }

    public void put(final String id, final Jsonable value) throws JSONException {
        final StringBuffer buffer = new StringBuffer();
        try {
            value.toJson(buffer);
            put(id, new JSONObject(buffer.toString()));
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DateTime lastUpdated() {
        final JSONObject jsonObject = get(LAST_UPDATED);
        if (jsonObject != null) {
            final String lastUpdated;
            try {
                lastUpdated = jsonObject.getString(LAST_UPDATED);
            } catch (final JSONException e) {
                throw new RuntimeException(e);
            }
            if (lastUpdated != null) {
                return new DateTime(lastUpdated);
            }
        }
        return null;
    }

    public void updateLastUpdated() {
        try {
            final JSONObject jsonObject = new JSONObject();
            jsonObject.put(LAST_UPDATED, new DateTime().toString());
            put(LAST_UPDATED, jsonObject);
        } catch (final JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void put(final String id, final JSONObject value) {

        if (isClosed) {
            throw new RuntimeException("Db is closed.");
        }

        // Prepare object
        final JsonFileDbEntry entry = new JsonFileDbEntry(id, System.currentTimeMillis(), value);

        try {
            // Store in log
            entry.toJson(printWriter);
            printWriter.println();
            printWriter.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }

        store(entry);

        if (!id.equals(LAST_UPDATED)) {
            updateLastUpdated();
        }

    }

    private void store(final JsonFileDbEntry value) {
        objectById.put(value.id, value);
    }

    public JSONObject get(final String id) {
        final JsonFileDbEntry jsonFileDbEntry = objectById.get(id);
        return jsonFileDbEntry != null ? jsonFileDbEntry.value : null;
    }

    public List<JSONObject> getAll(final String id) {
        final List<JSONObject> collect = objectById.values().stream()
                .map(jsonFileDbEntry -> jsonFileDbEntry.value).collect(Collectors.toList());
        return collect;
    }

    public Set<Map.Entry<String, JSONObject>> entries() {
        final Set<Map.Entry<String, JSONObject>> entries = new HashSet<Map.Entry<String, JSONObject>>();
        for (final String key : objectById.keySet()) {
            if (!key.equals(LAST_UPDATED) && get(key) != null) {
                entries.add(new AbstractMap.SimpleEntry<String, JSONObject>(key, get(key)));
            }
        }
        return entries;
    }

    @Override
    public Set<String> keys() {
        final Set<String> keys = new HashSet<String>(objectById.keySet());
        keys.remove(LAST_UPDATED);
        return keys;
    }

    public void close() {
        printWriter.close();
        isClosed = true;
    }

}
