package com.drwtrading.london.reddal.fastui;

import java.util.List;

class StringBatcher {

    private final String command;

    private String pendingValue;
    private String value;

    StringBatcher(final String command) {

        this.command = command;
        this.pendingValue = "";
        this.value = "";
    }

    void put(final String value) {
        pendingValue = value;
    }

    void flushPendingIntoCommandList(final List<String> commands) {
        if (!pendingValue.equals(value)) {
            commands.add(UiPipeImpl.cmd(this.command, pendingValue));
            value = pendingValue;
        }
    }

    void clear() {
        pendingValue = "";
        value = "";
    }

}
