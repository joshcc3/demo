package com.drwtrading.london.reddal.fastui;

import com.drwtrading.london.reddal.util.FastUtilCollections;

import java.util.List;

class ListBatcher {

    private final String command;
    private final StringBuilder cmdSB;

    private final List<String> pendingValues;

    ListBatcher(final String command) {

        this.command = command;
        this.cmdSB = new StringBuilder();

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
        final String pendingCmds = UiPipeImpl.cmd(cmdSB, pendingValues.toArray());
        return UiPipeImpl.cmd(cmdSB, this.command, pendingCmds);
    }

    void clear() {
        pendingValues.clear();
    }
}
