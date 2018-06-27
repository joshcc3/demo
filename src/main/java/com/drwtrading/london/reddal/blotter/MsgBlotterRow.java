package com.drwtrading.london.reddal.blotter;

class MsgBlotterRow implements Comparable<MsgBlotterRow> {

    final int id;
    private final long nanoSinceMidnight;

    final String timestamp;
    final String source;
    final String text;

    MsgBlotterRow(final int id, final long nanoSinceMidnight, final String timestamp, final String source, final String text) {

        this.id = id;
        this.nanoSinceMidnight = nanoSinceMidnight;

        this.timestamp = timestamp;
        this.source = source;
        this.text = text;
    }

    @Override
    public int hashCode() {

        return Long.hashCode(nanoSinceMidnight);
    }

    @Override
    public boolean equals(final Object o) {

        return this == o || (null != o && getClass() == o.getClass() && equals((MsgBlotterRow) o));
    }

    private boolean equals(final MsgBlotterRow that) {
        return nanoSinceMidnight == that.nanoSinceMidnight && text.equals(that.text);
    }

    @Override
    public int compareTo(final MsgBlotterRow o) {

        if (nanoSinceMidnight < o.nanoSinceMidnight) {
            return -1;
        } else if (o.nanoSinceMidnight < nanoSinceMidnight) {
            return 1;
        } else {
            return text.compareTo(o.text);
        }
    }
}
