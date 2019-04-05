package com.drwtrading.london.reddal.picard;

public interface ILiquidityFinderView {

    public void set(final String side, final String symbol, final String displaySymbol, final String bpsAway);

    public void remove(final String side, final String symbol);
}
