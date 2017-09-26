package com.drwtrading.london.reddal.stacks;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class StackIncreaseChildOffsetCmd {

    public final String source;
    public final String childName;
    public final BookSide side;
    public final double offsetIncreaseBPS;

    public StackIncreaseChildOffsetCmd(final String source, final String childName, final BookSide side, final double offsetIncreaseBPS) {

        this.source = source;
        this.childName = childName;
        this.side = side;
        this.offsetIncreaseBPS = offsetIncreaseBPS;
    }
}
