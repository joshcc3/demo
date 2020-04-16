package com.drwtrading.london.reddal.ladders.settings;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.london.reddal.util.TinyStringDb;
import org.jetlang.channels.Publisher;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;

public class LadderSettings {

    private final Publisher<LadderSettingsPrefLoaded> ladderPrefLoadedPublisher;

    private final TinyStringDb tinyStringDb;

    public LadderSettings(final Path dataFile, final Publisher<LadderSettingsPrefLoaded> ladderPrefLoadedPublisher) throws IOException {

        this.ladderPrefLoadedPublisher = ladderPrefLoadedPublisher;
        this.tinyStringDb = new TinyStringDb(dataFile);
    }

    public void load() {
        for (final Map.Entry<String, String> entry : tinyStringDb.entries()) {
            ladderPrefLoadedPublisher.publish(new LadderSettingsPrefLoaded(LadderSettingsPref.from(entry.getKey(), entry.getValue())));
        }
    }

    @Subscribe
    public void on(final LadderSettingsStoreLadderPref storeLadderPref) {

        tinyStringDb.put(storeLadderPref.pref.key(), storeLadderPref.pref.value);
        final LadderSettingsPrefLoaded msg = new LadderSettingsPrefLoaded(
                LadderSettingsPref.from(storeLadderPref.pref.key(), tinyStringDb.get(storeLadderPref.pref.key())));
        ladderPrefLoadedPublisher.publish(msg);
    }

}

