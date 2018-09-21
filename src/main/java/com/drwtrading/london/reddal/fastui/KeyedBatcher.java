package com.drwtrading.london.reddal.fastui;

import java.util.HashMap;
import java.util.Map;

class KeyedBatcher implements ICmdAppender {

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

    @Override
    public boolean appendCommand(final StringBuilder cmdSB, final char separator) {

        if (!pendingValues.isEmpty()) {

            appendCmd(cmdSB, separator);

            values.putAll(pendingValues);
            pendingValues.clear();

            return true;
        } else {
            return false;
        }
    }

    private void appendCmd(final StringBuilder cmdSB, final char separator) {

        cmdSB.append(command);
        for (final Map.Entry<String, String> entry : pendingValues.entrySet()) {

            cmdSB.append(separator);
            cmdSB.append(entry.getKey());

            cmdSB.append(separator);
            cmdSB.append(entry.getValue());
        }
    }

    void clear() {
        values.clear();
        pendingValues.clear();
    }
}
