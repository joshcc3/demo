package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class StacksDisableSiblingsCmd {

    public final String source;
    public final String familyName;
    public final BookSide side;

    public StacksDisableSiblingsCmd(final String source, final String familyName, final BookSide side) {

        this.source = source;
        this.familyName = familyName;
        this.side = side;
    }
}
