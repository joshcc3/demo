package com.drwtrading.london.reddal.trades;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

public class MrChillTrade {

    public final String symbol;
    public final BookSide side;
    public final long price;

    MrChillTrade(final String symbol, final BookSide side, final long price) {
        this.symbol = symbol;
        this.side = side;
        this.price = price;
    }
}
