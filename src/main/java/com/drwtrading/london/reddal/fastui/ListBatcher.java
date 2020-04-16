package com.drwtrading.london.reddal.fastui;

import java.util.ArrayList;
import java.util.List;

class ListBatcher implements ICmdAppender {

    private final String command;

    private final List<String> pendingValues;

    ListBatcher(final String command) {

        this.command = command;
        this.pendingValues = new ArrayList<>();
    }

    void put(final String value) {
        pendingValues.add(value);
    }

    @Override
    public boolean appendCommand(final StringBuilder cmdSB, final char separator) {

        if (!pendingValues.isEmpty()) {

            cmdSB.append(this.command);

            for (final String part : pendingValues) {
                cmdSB.append(separator);
                cmdSB.append(part);
            }
            pendingValues.clear();
            return true;
        } else {
            return false;
        }
    }

    void clear() {
        pendingValues.clear();
    }
}
