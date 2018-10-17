package com.drwtrading.london.reddal.util;

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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class TinyJsonDb implements TinyDb<JSONObject> {

    public static class JsonFileDbEntry implements Jsonable {

        public final String id;
        public final Long time;
        public final JSONObject value;
        final boolean deleted;

        JsonFileDbEntry(final String id, final Long time, final JSONObject data, final boolean deleted) {
            this.id = id;
            this.time = time;
            this.value = data;
            this.deleted = deleted;
        }

        static JsonFileDbEntry fromJson(final JSONObject jsonObject) throws JSONException {
            return new JsonFileDbEntry(jsonObject.getString("id"), jsonObject.getLong("time"), jsonObject.getJSONObject("value"),
                    jsonObject.has("deleted") && jsonObject.getBoolean("deleted"));
        }

        @Override
        public void toJson(final Appendable out) throws IOException {
            final JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", id);
                jsonObject.put("time", time);
                jsonObject.put("value", value);
                jsonObject.put("deleted", deleted);
                out.append(jsonObject.toString());
            } catch (final JSONException ignored) {
            }
        }
    }

    private final File dataFile;
    private final Map<String, JsonFileDbEntry> objectById = new HashMap<>();
    private final PrintWriter printWriter;
    private boolean isLoaded = false;
    private boolean isClosed = false;

    public TinyJsonDb(final Path dataFile) throws IOException {
        this.dataFile = dataFile.toFile();
        printWriter = new PrintWriter(new FileWriter(this.dataFile, true));
        load();
    }

    public void load() throws IOException {
        if (dataFile.exists()) {
            final List<String> strings = Files.readLines(dataFile, Charset.defaultCharset());
            for (final String string : strings) {
                try {
                    final JSONObject lineObj = new JSONObject(string);
                    final JsonFileDbEntry entry = JsonFileDbEntry.fromJson(lineObj);
                    if (entry.id.equals("__lastUpdated")) {
                        continue;
                    }
                    if (entry.deleted) {
                        objectById.remove(entry.id);
                    } else {
                        store(entry);
                    }
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
        return null;
    }

    @Override
    public void put(final String id, final JSONObject value) {

        if (isClosed) {
            throw new RuntimeException("Db is closed.");
        }

        // Prepare object
        final JsonFileDbEntry entry = new JsonFileDbEntry(id, System.currentTimeMillis(), value, false);

        write(entry);
        store(entry);

    }

    private void write(final JsonFileDbEntry entry) {
        try {
            // Store in log
            entry.toJson(printWriter);
            printWriter.println();
            printWriter.flush();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void remove(final String id) {
        final JsonFileDbEntry oldEntry = objectById.remove(id);
        if (null != oldEntry) {
            write(new JsonFileDbEntry(oldEntry.id, System.currentTimeMillis(), oldEntry.value, true));
        }
    }

    private void store(final JsonFileDbEntry value) {
        objectById.put(value.id, value);
    }

    @Override
    public JSONObject get(final String id) {
        final JsonFileDbEntry jsonFileDbEntry = objectById.get(id);
        return jsonFileDbEntry != null ? jsonFileDbEntry.value : null;
    }

    @Override
    public List<JSONObject> getAll(final String id) {
        return objectById.values().stream().map(jsonFileDbEntry -> jsonFileDbEntry.value).collect(Collectors.toList());
    }

    @Override
    public Set<Map.Entry<String, JSONObject>> entries() {
        final Set<Map.Entry<String, JSONObject>> entries = new HashSet<>();
        for (final String key : objectById.keySet()) {
            if (get(key) != null) {
                entries.add(new AbstractMap.SimpleEntry<>(key, get(key)));
            }
        }
        return entries;
    }

    @Override
    public Set<String> keys() {
        return new HashSet<>(objectById.keySet());
    }

}
