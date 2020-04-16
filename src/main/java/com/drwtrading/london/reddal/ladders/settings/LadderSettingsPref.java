package com.drwtrading.london.reddal.ladders.settings;

import com.drwtrading.london.util.Struct;
import com.google.common.base.Joiner;

public class LadderSettingsPref extends Struct {

    public static final String SEPARATOR = "<>";
    public final String user;
    public final String symbol;
    public final String id;
    public final String value;

    public LadderSettingsPref(final String user, final String symbol, final String id, final String value) {

        this.user = user;
        this.symbol = symbol;
        this.id = id;
        this.value = value;
    }

    public static LadderSettingsPref from(final String key, final String value) {

        final String[] split = key.split(SEPARATOR);
        return new LadderSettingsPref(split[0], split[1], split[2], value);
    }

    public String key() {
        return Joiner.on(SEPARATOR).join(user, symbol, id);
    }
}
