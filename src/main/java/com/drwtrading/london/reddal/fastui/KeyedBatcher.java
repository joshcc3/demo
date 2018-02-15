package com.drwtrading.london.reddal.fastui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class KeyedBatcher {

    private final String command;

    private final Map<String, String> values;
    private final Map<String, String> pendingValues;

    KeyedBatcher(final String command) {

        this.command = command;
        this.values = new HashMap<>();
        this.pendingValues = new HashMap<>();
    }

    void put(final String key, final String value) {

        if (!value.equals(values.get(key))) {
            pendingValues.put(key, value);
        } else {
            pendingValues.remove(key);
        }
    }

    void flushPendingIntoCommandList(final List<String> commands) {

        if (!pendingValues.isEmpty()) {
            commands.add(getCommand());
            values.putAll(pendingValues);
            pendingValues.clear();
        }
    }

    String getCommand() {

        final List<String> updates = new ArrayList<>();
        for (final Map.Entry<String, String> entry : pendingValues.entrySet()) {
            updates.add(entry.getKey());
            updates.add(entry.getValue());
        }
        return UiPipeImpl.cmd(this.command, UiPipeImpl.cmd(updates.toArray()));
    }

    void clear() {
        values.clear();
        pendingValues.clear();
    }
}
