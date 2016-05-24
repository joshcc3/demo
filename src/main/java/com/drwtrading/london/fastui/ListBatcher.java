package com.drwtrading.london.fastui;

import com.drwtrading.london.reddal.util.FastUtilCollections;

import java.util.List;

class ListBatcher {

    private final String command;
    private final List<String> pendingValues;

    ListBatcher(final String command) {

        this.command = command;
        this.pendingValues = FastUtilCollections.newFastList();
    }

    void put(final String value) {
        pendingValues.add(value);
    }

    void flushPendingIntoCommandList(final List<String> commands) {
        if (!pendingValues.isEmpty()) {
            commands.add(getCommand());
            pendingValues.clear();
        }
    }

    String getCommand() {
        return UiPipeImpl.cmd(this.command, UiPipeImpl.cmd(pendingValues.toArray()));
    }

    void clear() {
        pendingValues.clear();
    }

}
