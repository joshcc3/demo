package com.drwtrading.london.reddal.blotter;

import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;

import java.text.DecimalFormat;

class SafetiesBlotterRow {

    final int id;
    final String source;
    final String safetyName;

    String lastSymbol;
    String limit;
    String warningLevel;

    String currentLevel;

    boolean isWarning;
    boolean isError;

    SafetiesBlotterRow(final int id, final String source, final ANibblerSafety<?> safety, final DecimalFormat longDF,
            final String currentLevel) {

        this.id = id;
        this.source = source;
        this.safetyName = safety.getSafetyName();

        this.lastSymbol = safety.getLastSymbol();
        this.limit = longDF.format(safety.getLimit());
        this.warningLevel = longDF.format(safety.getWarningLevel());

        this.currentLevel = currentLevel;

        this.isWarning = safety.isWarning();
        this.isError = safety.isError();
    }

    void update(final ANibblerSafety<?> safety, final DecimalFormat longDF, final String currentLevel) {

        this.lastSymbol = safety.getLastSymbol();
        this.limit = longDF.format(safety.getLimit());
        this.warningLevel = longDF.format(safety.getWarningLevel());

        this.currentLevel = currentLevel;

        this.isWarning = safety.isWarning();
        this.isError = safety.isError();
    }
}
