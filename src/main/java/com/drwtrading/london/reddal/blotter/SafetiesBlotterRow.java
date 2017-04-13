package com.drwtrading.london.reddal.blotter;

import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;

class SafetiesBlotterRow {

    final int id;
    final String source;
    final String safetyName;

    String lastSymbol;
    long limit;
    long warningLevel;

    String currentLevel;

    boolean isWarning;
    boolean isError;

    SafetiesBlotterRow(final int id, final String source, final ANibblerSafety<?> safety, final String currentLevel) {

        this.id = id;
        this.source = source;
        this.safetyName = safety.getSafetyName();

        this.lastSymbol = safety.getLastSymbol();
        this.limit = safety.getLimit();
        this.warningLevel = safety.getWarningLevel();

        this.currentLevel = currentLevel;

        this.isWarning = safety.isWarning();
        this.isError = safety.isError();
    }

    void update(final ANibblerSafety<?> safety, final String currentLevel) {

        this.lastSymbol = safety.getLastSymbol();
        this.limit = safety.getLimit();
        this.warningLevel = safety.getWarningLevel();

        this.currentLevel = currentLevel;

        this.isWarning = safety.isWarning();
        this.isError = safety.isError();
    }
}
