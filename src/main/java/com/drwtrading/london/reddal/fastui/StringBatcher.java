package com.drwtrading.london.reddal.fastui;

class StringBatcher implements ICmdAppender {

    private final String command;
    private final StringBuilder cmdSB;

    private String pendingValue;
    private String value;

    StringBatcher(final String command) {

        this.command = command;
        this.cmdSB = new StringBuilder();

        this.pendingValue = "";
        this.value = "";
    }

    void put(final String value) {
        pendingValue = value;
    }

    @Override
    public boolean appendCommand(final StringBuilder cmdSB, final char separator) {

        if (!pendingValue.equals(value)) {
            cmdSB.append(command);
            cmdSB.append(separator);
            cmdSB.append(pendingValue);
            value = pendingValue;
            return true;
        } else {
            return false;
        }
    }

    void clear() {
        pendingValue = "";
        value = "";
    }
}
