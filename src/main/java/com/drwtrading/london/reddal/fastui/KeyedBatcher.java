package com.drwtrading.london.reddal.fastui;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class KeyedBatcher {

    private final String command;
    private final StringBuilder cmdSB;

    private final Map<String, String> values;
    private final Map<String, String> pendingValues;

    KeyedBatcher(final String command) {

        this.command = command;
        this.cmdSB = new StringBuilder();

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
        final String updateCmds = UiPipeImpl.cmd(cmdSB, updates.toArray());
        return UiPipeImpl.cmd(cmdSB, this.command, updateCmds);
    }

    void clear() {
        values.clear();
        pendingValues.clear();
    }
}
