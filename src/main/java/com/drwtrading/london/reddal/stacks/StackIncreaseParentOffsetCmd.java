package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class StackIncreaseParentOffsetCmd {

    public final String source;
    public final String familyName;
    public final BookSide side;
    public final int multiplier;

    public StackIncreaseParentOffsetCmd(final String source, final String familyName, final BookSide side, final int multiplier) {

        this.source = source;
        this.familyName = familyName;
        this.side = side;
        this.multiplier = multiplier;
    }
}
