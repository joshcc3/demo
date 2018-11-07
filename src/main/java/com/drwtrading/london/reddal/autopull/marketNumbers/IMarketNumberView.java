package com.drwtrading.london.reddal.autopull.marketNumbers;

public interface IMarketNumberView {

    public void clear();

    public void add(final String description, final String londonTime);
}
