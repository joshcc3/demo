package com.drwtrading.london.reddal.blotter;

import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;

import java.text.DecimalFormat;

class SafetiesBlotterRow {

    final int id;

    final String source;
    final long remoteSafetyID;
    final String safetyName;

    String lastSymbol;
    String limit;
    String warningLevel;

    String currentLevel;

    boolean isEditable;
    boolean isWarning;
    boolean isError;

    SafetiesBlotterRow(final int id, final String source, final ANibblerSafety<?> safety, final DecimalFormat longDF,
            final String currentLevel) {

        this.id = id;
        this.source = source;
        this.remoteSafetyID = safety.getSafetyID();
        this.safetyName = safety.getSafetyName();

        update(safety, longDF, currentLevel);
    }

    void update(final ANibblerSafety<?> safety, final DecimalFormat longDF, final String currentLevel) {

        this.lastSymbol = safety.getLastSymbol();
        this.limit = longDF.format(safety.getLimit());
        this.warningLevel = longDF.format(safety.getWarningLevel());

        this.currentLevel = currentLevel;

        this.isEditable = safety.canRemotelyUpdateLimit();
        this.isWarning = safety.isWarning();
        this.isError = safety.isError();
    }
}
