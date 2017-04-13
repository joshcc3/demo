package com.drwtrading.london.reddal.blotter;

class MsgBlotterRow implements Comparable<MsgBlotterRow> {

    final int id;
    final long nanoSinceMidnight;

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

        if (this == o) {
            return true;
        } else if (o == null || getClass() != o.getClass()) {
            return false;
        } else {
            final MsgBlotterRow that = (MsgBlotterRow) o;
            return nanoSinceMidnight == that.nanoSinceMidnight && source.equals(that.source) && text.equals(that.text);
        }
    }

    @Override
    public int compareTo(final MsgBlotterRow o) {
        if (equals(o)) {
            return 0;
        } else if (nanoSinceMidnight < o.nanoSinceMidnight) {
            return -1;
        } else if (o.nanoSinceMidnight < nanoSinceMidnight) {
            return 1;
        } else {
            return Integer.compare(id, o.id);
        }
    }
}
