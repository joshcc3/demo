package com.drwtrading.london.reddal;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.util.TinyStringDb;
import com.drwtrading.london.util.Struct;
import com.google.common.base.Joiner;
import org.jetlang.channels.Publisher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class LadderSettings {

    private final Publisher<LadderPrefLoaded> ladderPrefLoadedPublisher;

    public static class LadderPref extends Struct {

        public static final String SEPARATOR = "<>";
        public final String user;
        public final String symbol;
        public final String id;
        public final String value;

        public LadderPref(final String user, final String symbol, final String id, final String value) {
            this.user = user;
            this.symbol = symbol;
            this.id = id;
            this.value = value;
        }

        public static LadderPref from(final String key, final String value) {
            final String[] split = key.split(SEPARATOR);
            return new LadderPref(split[0], split[1], split[2], value);
        }

        public String key() {
            return Joiner.on(SEPARATOR).join(user, symbol, id);
        }
    }

    public static class LadderPrefLoaded extends Struct {

        public final LadderPref pref;

        public LadderPrefLoaded(final LadderPref pref) {
            this.pref = pref;
        }
    }

    public static class StoreLadderPref extends Struct {

        public final LadderPref pref;

        public StoreLadderPref(final LadderPref pref) {
            this.pref = pref;
        }
    }

    private final TinyStringDb tinyStringDb;

    public LadderSettings(final Path dataFile, final Publisher<LadderPrefLoaded> ladderPrefLoadedPublisher) throws IOException {
        this.ladderPrefLoadedPublisher = ladderPrefLoadedPublisher;
        tinyStringDb = new TinyStringDb(dataFile);
    }

    public void load() {
        for (final Map.Entry<String, String> entry : tinyStringDb.entries()) {
            ladderPrefLoadedPublisher.publish(new LadderPrefLoaded(LadderPref.from(entry.getKey(), entry.getValue())));
        }
    }

    @Subscribe
    public void on(final StoreLadderPref storeLadderPref) {
        // Round-trip through the database
        tinyStringDb.put(storeLadderPref.pref.key(), storeLadderPref.pref.value);
        ladderPrefLoadedPublisher.publish(
                new LadderPrefLoaded(LadderPref.from(storeLadderPref.pref.key(), tinyStringDb.get(storeLadderPref.pref.key()))));
    }

}
