package com.drwtrading.london.reddal.ladders;

import java.util.ArrayList;

class LadderHTMLTable {

    private final ArrayList<LadderHTMLRow> ladderHTMLRows;

    LadderHTMLTable() {

        this.ladderHTMLRows = new ArrayList<>();
    }

    void extendToLevels(final int levels) {

        for (int i = ladderHTMLRows.size(); i < levels; ++i) {

            final LadderHTMLRow row = new LadderHTMLRow(i);
            ladderHTMLRows.add(i, row);
        }
    }

    LadderHTMLRow getRow(final int level) {
        return ladderHTMLRows.get(level);
    }
}
