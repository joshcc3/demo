package com.drwtrading.london.reddal.stacks.opxl;

import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;

public class StackRefPriceDetail {

    public final String symbol;
    public final long refPrice;
    public final ITickTable tickTable;

    public StackRefPriceDetail(final String symbol, final long refPrice, final ITickTable tickTable) {

        this.symbol = symbol;
        this.refPrice = refPrice;
        this.tickTable = tickTable;
    }
}
