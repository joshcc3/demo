package com.drwtrading.london.reddal.util;

import drw.london.json.Jsonable;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TinyJsonDb {

    private final Path dataFile;

    private final Map<String, TinyJsonFileDbEntry> objectById = new HashMap<>();
    private final PrintWriter printWriter;

    public TinyJsonDb(final Path dataFile) throws IOException {

        this.dataFile = dataFile;

        final Path dir = dataFile.getParent();
        Files.createDirectories(dir);

        final Writer writer =
                Files.newBufferedWriter(dataFile, StandardOpenOption.CREATE, StandardOpenOption.APPEND, StandardOpenOption.WRITE);
        this.printWriter = new PrintWriter(writer);
        load();
    }

    public void load() throws IOException {

        if (Files.exists(dataFile)) {

            final List<String> strings = Files.readAllLines(dataFile, Charset.defaultCharset());
            for (final String string : strings) {
                try {
                    final JSONObject lineObj = new JSONObject(string);
                    final TinyJsonFileDbEntry entry = TinyJsonFileDbEntry.fromJson(lineObj);
                    if (!"__lastUpdated".equals(entry.id)) {
                        if (entry.deleted) {
                            objectById.remove(entry.id);
                        } else {
                            store(entry);
                        }
                    }
                } catch (final JSONException e) {
                    System.out.println("Error parsing line: " + string);
                    e.printStackTrace();
                }
            }
        }
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

    public void put(final String id, final JSONObject value) {

        final TinyJsonFileDbEntry entry = new TinyJsonFileDbEntry(id, System.currentTimeMillis(), value, false);

        write(entry);
        store(entry);
    }

    private void write(final TinyJsonFileDbEntry entry) {

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

        final TinyJsonFileDbEntry oldEntry = objectById.remove(id);
        if (null != oldEntry) {
            write(new TinyJsonFileDbEntry(oldEntry.id, System.currentTimeMillis(), oldEntry.value, true));
        }
    }

    private void store(final TinyJsonFileDbEntry value) {

        objectById.put(value.id, value);
    }

    public JSONObject get(final String id) {

        final TinyJsonFileDbEntry jsonFileDbEntry = objectById.get(id);
        return jsonFileDbEntry != null ? jsonFileDbEntry.value : null;
    }

    public Set<Map.Entry<String, JSONObject>> entries() {

        final Set<Map.Entry<String, JSONObject>> entries = new HashSet<>();
        for (final String key : objectById.keySet()) {
            if (get(key) != null) {
                entries.add(new AbstractMap.SimpleEntry<>(key, get(key)));
            }
        }
        return entries;
    }

    public Set<String> keys() {
        return new HashSet<>(objectById.keySet());
    }
}
