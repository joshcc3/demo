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
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TinyJsonDb implements TinyDb<JSONObject> {

    public static final String LAST_UPDATED = "__lastUpdated";

    public static class JsonFileDbEntry implements Jsonable {

        public final String id;
        public final Long time;
        public final JSONObject value;

        public JsonFileDbEntry(String id, Long time, JSONObject data) {
            this.id = id;
            this.time = time;
            this.value = data;
        }

        public static JsonFileDbEntry fromJson(JSONObject jsonObject) throws JSONException {
            return new JsonFileDbEntry(
                    jsonObject.getString("id"),
                    jsonObject.getLong("time"),
                    jsonObject.getJSONObject("value"));
        }

        @Override
        public void toJson(Appendable out) throws IOException {
            JSONObject jsonObject = new JSONObject();
            try {
                jsonObject.put("id", id);
                jsonObject.put("time", time);
                jsonObject.put("value", value);
                out.append(jsonObject.toString());
            } catch (JSONException e) {
                return;
            }
        }
    }

    public final File dataFile;
    public final Map<String, List<JsonFileDbEntry>> objectById = new MapMaker().makeComputingMap(new Function<String, List<JsonFileDbEntry>>() {
        @Override
        public List<JsonFileDbEntry> apply(String from) {
            return new ArrayList<JsonFileDbEntry>();
        }
    });
    public final PrintWriter printWriter;
    public boolean isLoaded = false;
    public boolean isClosed = false;

    public TinyJsonDb(File dataFile) throws IOException {
        this.dataFile = dataFile;
        printWriter = new PrintWriter(new FileWriter(this.dataFile, true));
        load();
    }


    public boolean isLoaded() {
        return isLoaded;
    }

    public void load() throws IOException {
        if (dataFile.exists()) {
            List<String> strings = Files.readLines(dataFile, Charset.defaultCharset());
            for (String string : strings) {
                try {
                    JSONObject lineObj = new JSONObject(string);
                    JsonFileDbEntry entry = JsonFileDbEntry.fromJson(lineObj);
                    store(entry);
                } catch (JSONException e) {
                    System.out.println("Error parsing line: " + string);
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        isLoaded = true;
    }

    public void put(String id, Jsonable value) throws JSONException {
        StringBuffer buffer = new StringBuffer();
        try {
            value.toJson(buffer);
            put(id, new JSONObject(buffer.toString()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public DateTime lastUpdated() {
        JSONObject jsonObject = get(LAST_UPDATED);
        if (jsonObject != null) {
            String lastUpdated;
            try {
                lastUpdated = jsonObject.getString(LAST_UPDATED);
            } catch (JSONException e) {
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
            JSONObject jsonObject = new JSONObject();
            jsonObject.put(LAST_UPDATED, new DateTime().toString());
            put(LAST_UPDATED, jsonObject);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
    }

    public void put(String id, JSONObject value) {

        if (isClosed) {
            throw new RuntimeException("Db is closed.");
        }

        JSONObject old = get(id);

        // Prepare object
        JsonFileDbEntry entry = new JsonFileDbEntry(id, System.currentTimeMillis(), value);

        try {
            // Store in log
            entry.toJson(printWriter);
            printWriter.println();
            printWriter.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        store(entry);

        if (!id.equals(LAST_UPDATED)) {
            updateLastUpdated();
        }

//        return old;
    }

    private void store(JsonFileDbEntry value) {
        objectById.get(value.id).add(value);
    }

    public JSONObject get(String id) {
        List<JsonFileDbEntry> objects = objectById.get(id);
        return objects.size() == 0 ? null : objects.get(objects.size() - 1).value;
    }

    public List<JSONObject> getAll(String id) {
        return new ArrayList<JSONObject>(Collections2.transform(objectById.get(id), new Function<JsonFileDbEntry, JSONObject>() {
            @Override
            public JSONObject apply(JsonFileDbEntry from) {
                return from.value;
            }
        }));
    }

    public Set<Map.Entry<String, JSONObject>> entries() {
        Set<Map.Entry<String, JSONObject>> entries = new HashSet<Map.Entry<String, JSONObject>>();
        for (String key : objectById.keySet()) {
            if (!key.equals(LAST_UPDATED) && get(key) != null) {
                entries.add(new AbstractMap.SimpleEntry<String, JSONObject>(key, get(key)));
            }
        }
        return entries;
    }

    @Override
    public Set<String> keys() {
        Set<String> keys = new HashSet<String>(objectById.keySet());
        keys.remove(LAST_UPDATED);
        return keys;
    }

    public void close() {
        printWriter.close();
        isClosed = true;
    }

}
