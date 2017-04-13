package com.drwtrading.london.reddal.blotter;

import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;
import com.drwtrading.london.eeif.utils.collections.LongMap;

class SafetiesBlotterBlock {

    final String source;

    final LongMap<SafetiesBlotterRow> safeties;

    boolean isConnected;

    SafetiesBlotterBlock(final String source, final boolean isConnected) {

        this.source = source;

        this.safeties = new LongMap<>();

        this.isConnected = isConnected;
    }

    SafetiesBlotterRow setSafety(final int id, final ANibblerSafety<?> safety, final String currentLevel) {

        final SafetiesBlotterRow row = new SafetiesBlotterRow(id, source, safety, currentLevel);
        safeties.put(safety.getSafetyID(), row);
        return row;
    }

    SafetiesBlotterRow updateSafety(final ANibblerSafety<?> safety, final String currentLevel) {

        final SafetiesBlotterRow row = safeties.get(safety.getSafetyID());
        row.update(safety, currentLevel);
        return row;
    }

    void clear() {
        safeties.clear();
    }
}
