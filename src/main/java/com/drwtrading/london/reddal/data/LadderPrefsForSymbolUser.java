package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.nibbler.transport.data.types.Tag;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.ladders.settings.LadderSettingsPref;
import com.drwtrading.london.reddal.ladders.settings.LadderSettingsPrefLoaded;
import com.drwtrading.london.reddal.ladders.settings.LadderSettingsStoreLadderPref;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class LadderPrefsForSymbolUser {

    private static final Set<LadderSettingsPref> globalDefaults = new HashSet<>();

    static {
        final LadderSettingsPref globalDefault = new LadderSettingsPref("*", "*", HTML.WORKING_ORDER_TAG, Tag.CHAD.name());
        globalDefaults.add(globalDefault);
    }

    public final String symbol;
    public final String user;
    private final Map<String, String> globalPrefs = new HashMap<>();
    private final Map<String, String> userPrefs = new HashMap<>();
    private final Map<String, String> symbolPrefs = new HashMap<>();
    private final Publisher<LadderSettingsStoreLadderPref> storeLadderPrefPublisher;

    public LadderPrefsForSymbolUser(final String symbol, final String user,
            final Publisher<LadderSettingsStoreLadderPref> storeLadderPrefPublisher) {

        this.symbol = symbol;
        this.user = user;
        this.storeLadderPrefPublisher = storeLadderPrefPublisher;
        for (final LadderSettingsPref globalDefault : globalDefaults) {
            on(new LadderSettingsPrefLoaded(globalDefault));
        }
    }

    public void on(final LadderSettingsPrefLoaded ladderPrefLoaded) {
        final LadderSettingsPref pref = ladderPrefLoaded.pref;
        if ("*".equals(pref.user) && "*".equals(pref.symbol)) {
            globalPrefs.put(pref.id, pref.value);
        } else if ("*".equals(pref.user) && pref.symbol.equals(symbol)) {
            userPrefs.put(pref.id, pref.value);
        } else if (pref.user.equals(user) && pref.symbol.equals(symbol)) {
            symbolPrefs.put(pref.id, pref.value);
        }
    }

    public String get(final String id) {
        if (symbolPrefs.containsKey(id)) {
            return symbolPrefs.get(id);
        } else if (userPrefs.containsKey(id)) {
            return userPrefs.get(id);
        } else {
            return globalPrefs.getOrDefault(id, null);
        }
    }

    public String get(final String id, final Object otherwise) {
        final String preference = get(id);
        if (preference != null) {
            return preference;
        }
        return otherwise.toString();
    }

    public void set(final String id, final Object value) {
        storeLadderPrefPublisher.publish(new LadderSettingsStoreLadderPref(new LadderSettingsPref(user, symbol, id, value.toString())));
    }

    public LadderPrefsForSymbolUser withSymbol(final String newSymbol) {
        final LadderPrefsForSymbolUser ladderPrefsForSymbolUser = new LadderPrefsForSymbolUser(newSymbol, user, storeLadderPrefPublisher);
        symbolPrefs.forEach(ladderPrefsForSymbolUser::set);
        ladderPrefsForSymbolUser.symbolPrefs.putAll(symbolPrefs);
        ladderPrefsForSymbolUser.userPrefs.putAll(userPrefs);
        ladderPrefsForSymbolUser.globalPrefs.putAll(globalPrefs);
        return ladderPrefsForSymbolUser;
    }

}
