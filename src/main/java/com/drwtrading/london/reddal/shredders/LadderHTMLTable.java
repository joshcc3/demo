package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.reddal.ladders.model.BookHTMLRow;

import java.util.ArrayList;

public class LadderHTMLTable {

    private final ArrayList<BookHTMLRow> ladderHTMLRows;

    public LadderHTMLTable() {

        this.ladderHTMLRows = new ArrayList<>();
    }

    public void extendToLevels(final int levels) {

        for (int i = ladderHTMLRows.size(); i < levels; ++i) {

            final BookHTMLRow row = new BookHTMLRow(i);
            ladderHTMLRows.add(i, row);
        }
    }

    public BookHTMLRow getRow(final int level) {
        return ladderHTMLRows.get(level);
    }
}
