package com.drwtrading.london.reddal.ladders;

import java.util.ArrayList;

public class LadderHTMLTable {

    private final ArrayList<LadderHTMLRow> ladderHTMLRows;

    public LadderHTMLTable() {

        this.ladderHTMLRows = new ArrayList<>();
    }

    public void extendToLevels(final int levels) {

        for (int i = ladderHTMLRows.size(); i < levels; ++i) {

            final LadderHTMLRow row = new LadderHTMLRow(i);
            ladderHTMLRows.add(i, row);
        }
    }

    public LadderHTMLRow getRow(final int level) {
        return ladderHTMLRows.get(level);
    }
}
