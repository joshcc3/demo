package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.util.TinyStringDb;
import com.drwtrading.london.util.Struct;
import org.jetlang.channels.Publisher;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LadderSettings {

    private final Publisher<LadderPrefLoaded> ladderPrefLoadedPublisher;
    public static class LadderPref extends Struct {

        public final String user;
        public final String symbol;
        public final String id;
        public final String value;

        public LadderPref(String user, String symbol, String id, String value) {
            this.user = user;
            this.symbol = symbol;
            this.id = id;
            this.value = value;
        }

        public static LadderPref from(String key, String value) {
            String[] split = key.split("\\.");
            return new LadderPref(split[0], split[1], split[2], value);
        }

        public String key() {
            return user + "." + symbol + "." + id;
        }
    }

    public static class LadderPrefLoaded extends Struct {
        public final LadderPref pref;

        public LadderPrefLoaded(LadderPref pref) {
            this.pref = pref;
        }
    }

    public static class StoreLadderPref extends Struct {
        public final LadderPref pref;

        public StoreLadderPref(LadderPref pref) {
            this.pref = pref;
        }
    }

    private final TinyStringDb tinyStringDb;

    public LadderSettings(File dataFile, Publisher<LadderPrefLoaded> ladderPrefLoadedPublisher) throws IOException {
        this.ladderPrefLoadedPublisher = ladderPrefLoadedPublisher;
        tinyStringDb = new TinyStringDb(dataFile);
    }

    public void load() {
        for (Map.Entry<String, String> entry : tinyStringDb.entries()) {
            ladderPrefLoadedPublisher.publish(new LadderPrefLoaded(LadderPref.from(entry.getKey(), entry.getValue())));
        }
    }

    public Runnable loadRunnable() {
        return new Runnable() {
            @Override
            public void run() {
                load();
            }
        };
    }

    @Subscribe
    public void on(StoreLadderPref storeLadderPref) {
        // Round-trip through the database
        tinyStringDb.put(storeLadderPref.pref.key(), storeLadderPref.pref.value);
        ladderPrefLoadedPublisher.publish(new LadderPrefLoaded(LadderPref.from(storeLadderPref.pref.key(), tinyStringDb.get(storeLadderPref.pref.key()))));
    }

}
