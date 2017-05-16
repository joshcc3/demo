package com.drwtrading.london.reddal.blotter;

import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerOMSEnabledState;
import com.drwtrading.london.eeif.utils.collections.LongMap;

import java.text.DecimalFormat;

class SafetiesBlotterBlock {

    private final DecimalFormat longDF;

    final String source;

    final LongMap<SafetiesOMSRow> omsEnabledStates;
    final LongMap<SafetiesBlotterRow> safeties;

    boolean isConnected;

    SafetiesBlotterBlock(final DecimalFormat longDF, final String source, final boolean isConnected) {

        this.longDF = longDF;

        this.source = source;
        this.omsEnabledStates = new LongMap<>();
        this.safeties = new LongMap<>();

        this.isConnected = isConnected;
    }

    SafetiesBlotterRow getRow(final int id) {
        return safeties.get(id);
    }

    SafetiesOMSRow createOMSEnabledState(final int id, final NibblerOMSEnabledState omsEnabledState) {

        final SafetiesOMSRow row = new SafetiesOMSRow(id, source, omsEnabledState);
        omsEnabledStates.put(omsEnabledState.getOMSID(), row);
        return row;
    }

    SafetiesOMSRow udpateOMSEnabledState(final NibblerOMSEnabledState omsEnabledState) {

        final SafetiesOMSRow row = omsEnabledStates.get(omsEnabledState.getOMSID());
        row.update(omsEnabledState);
        return row;
    }

    SafetiesBlotterRow createSafety(final int id, final ANibblerSafety<?> safety, final String currentLevel) {

        final SafetiesBlotterRow row = new SafetiesBlotterRow(id, source, safety, longDF, currentLevel);
        safeties.put(safety.getSafetyID(), row);
        return row;
    }

    SafetiesBlotterRow updateSafety(final ANibblerSafety<?> safety, final String currentLevel) {

        final SafetiesBlotterRow row = safeties.get(safety.getSafetyID());
        row.update(safety, longDF, currentLevel);
        return row;
    }

    void clear() {
        safeties.clear();
    }
}
