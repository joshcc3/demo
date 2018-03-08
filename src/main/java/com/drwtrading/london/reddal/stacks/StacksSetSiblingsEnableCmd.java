package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class StacksSetSiblingsEnableCmd {

    public final String source;
    public final String familyName;
    public final BookSide side;

    public final boolean isEnabled;

    public StacksSetSiblingsEnableCmd(final String source, final String familyName, final BookSide side, final boolean isEnabled) {

        this.source = source;
        this.familyName = familyName;
        this.side = side;

        this.isEnabled = isEnabled;
    }
}
