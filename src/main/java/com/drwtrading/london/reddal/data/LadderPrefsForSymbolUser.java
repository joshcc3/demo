package com.drwtrading.london.reddal.data;

import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.ladders.LadderSettings;
import org.jetlang.channels.Publisher;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static com.drwtrading.london.reddal.util.FastUtilCollections.newFastMap;

public class LadderPrefsForSymbolUser {

    public static final Set<LadderSettings.LadderPref> globalDefaults = new HashSet<>();

    static {
        globalDefaults.add(new LadderSettings.LadderPref("*", "*", HTML.WORKING_ORDER_TAG, "CHAD"));
    }

    public final String symbol;
    public final String user;

    private final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher;

    public final Map<String, String> globalPrefs = newFastMap();
    public final Map<String, String> userPrefs = newFastMap();
    public final Map<String, String> symbolPrefs = newFastMap();

    public LadderPrefsForSymbolUser(final String symbol, final String user,
                                    final Publisher<LadderSettings.StoreLadderPref> storeLadderPrefPublisher) {
        this.symbol = symbol;
        this.user = user;
        this.storeLadderPrefPublisher = storeLadderPrefPublisher;
        for (final LadderSettings.LadderPref globalDefault : globalDefaults) {
            on(new LadderSettings.LadderPrefLoaded(globalDefault));
        }
    }

    public void on(final LadderSettings.LadderPrefLoaded ladderPrefLoaded) {
        final LadderSettings.LadderPref pref = ladderPrefLoaded.pref;
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
        } else if (globalPrefs.containsKey((id))) {
            return globalPrefs.get(id);
        } else {
            return null;
        }
    }

    public String get(final String id, final Object otherwise) {
        if (get(id) != null) {
            return get(id);
        }
        return otherwise.toString();
    }

    public void set(final String id, final Object value) {
        storeLadderPrefPublisher.publish(
                new LadderSettings.StoreLadderPref(new LadderSettings.LadderPref(user, symbol, id, value.toString())));
    }

    public LadderPrefsForSymbolUser withSymbol(String newSymbol) {
        LadderPrefsForSymbolUser ladderPrefsForSymbolUser = new LadderPrefsForSymbolUser(newSymbol, user, storeLadderPrefPublisher);
        symbolPrefs.forEach(ladderPrefsForSymbolUser::set);
        ladderPrefsForSymbolUser.symbolPrefs.putAll(symbolPrefs);
        ladderPrefsForSymbolUser.userPrefs.putAll(userPrefs);
        ladderPrefsForSymbolUser.globalPrefs.putAll(globalPrefs);
        return ladderPrefsForSymbolUser;
    }

}
