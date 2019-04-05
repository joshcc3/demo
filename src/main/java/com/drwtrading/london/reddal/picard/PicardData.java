package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;

class PicardData {

    final String symbol;
    final MDForSymbol mdForSymbol;

    final PicardDistanceData invalidBidLaserDistanceRow;
    final PicardDistanceData invalidAskLaserDistanceRow;

    LaserLineValue bidLaserLine;
    LaserLineValue askLaserLine;

    PicardRow previousRow;

    PicardDistanceData bidLaserDistance;
    PicardDistanceData askLaserDistance;

    PicardData(final String symbol, final MDForSymbol mdForSymbol) {

        this.symbol = symbol;
        this.mdForSymbol = mdForSymbol;

        this.invalidBidLaserDistanceRow = new PicardDistanceData(symbol, false, BookSide.BID, Long.MAX_VALUE);
        this.invalidAskLaserDistanceRow = new PicardDistanceData(symbol, false, BookSide.ASK, Long.MAX_VALUE);

        this.bidLaserDistance = invalidBidLaserDistanceRow;
        this.askLaserDistance = invalidAskLaserDistanceRow;
    }
}
