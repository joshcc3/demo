package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.util.Struct;

public class PicardRow extends Struct {

    public final long milliSinceMidnight;
    public final String symbol;
    public final InstType instType;

    public final BookSide side;
    public final long price;
    public final String prettyPrice;
    public final double bpsThrough;

    public final PicardRowState state;
    public final String description;

    public final boolean inAuction;
    public final boolean isNewRow;

    public PicardRow(final PicardRow oldRow, final PicardRowState newState) {
        this(oldRow.milliSinceMidnight, oldRow.symbol, oldRow.instType, oldRow.side, oldRow.price, oldRow.prettyPrice, oldRow.bpsThrough,
                newState, oldRow.description, oldRow.inAuction, false);
    }

    public PicardRow(final long milliSinceMidnight, final String symbol, final InstType instType, final BookSide side, final long price,
            final String prettyPrice, final double bpsThrough, final PicardRowState state, final String description,
            final boolean inAuction, final boolean isNewRow) {

        this.milliSinceMidnight = milliSinceMidnight;
        this.symbol = symbol;
        this.instType = instType;

        this.price = price;
        this.prettyPrice = prettyPrice;
        this.side = side;
        this.bpsThrough = bpsThrough;

        this.description = description;
        this.state = state;

        this.inAuction = inAuction;
        this.isNewRow = isNewRow;
    }
}
