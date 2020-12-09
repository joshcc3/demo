package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.data.LaserLine;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;

class PicardData {

    final String symbol;
    final MDForSymbol mdForSymbol;

    final LiquidityFinderData invalidBidLaserDistanceRow;
    final LiquidityFinderData invalidAskLaserDistanceRow;

    LaserLine bidLaserLine;
    LaserLine askLaserLine;

    PicardRowWithInstID previousRow;

    LiquidityFinderData bidLaserDistance;
    LiquidityFinderData askLaserDistance;

    PicardData(final String symbol, final MDForSymbol mdForSymbol) {

        this.symbol = symbol;
        this.mdForSymbol = mdForSymbol;

        this.invalidBidLaserDistanceRow = new LiquidityFinderData(symbol, false, BookSide.BID, Long.MAX_VALUE);
        this.invalidAskLaserDistanceRow = new LiquidityFinderData(symbol, false, BookSide.ASK, Long.MAX_VALUE);

        this.bidLaserDistance = invalidBidLaserDistanceRow;
        this.askLaserDistance = invalidAskLaserDistanceRow;
    }
}
