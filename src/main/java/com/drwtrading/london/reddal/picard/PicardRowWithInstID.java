package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.InstType;

public class PicardRowWithInstID extends PicardRow {

    public final InstrumentID instrumentID;

    public PicardRowWithInstID(final PicardRowWithInstID oldRow, final PicardRowState newState) {
        this(oldRow.milliSinceMidnight, oldRow.symbol, oldRow.instrumentID, oldRow.instType, oldRow.ccy, oldRow.side, oldRow.price, oldRow.prettyPrice, oldRow.bpsThrough,
                oldRow.opportunitySize, newState, oldRow.description, oldRow.inAuction, false);
    }

    public PicardRowWithInstID(final long milliSinceMidnight, final String symbol, final InstrumentID instrumentID, final InstType instType, final CCY ccy, final BookSide side, final long price,
            final String prettyPrice, final double bpsThrough, final double opportunitySize, final PicardRowState state, final String description,
            final boolean inAuction, final boolean isNewRow) {

        super(milliSinceMidnight, symbol, instType, ccy, side, price, prettyPrice, bpsThrough, opportunitySize, state, description,
                inAuction, isNewRow);

        this.instrumentID = instrumentID;
    }
}
