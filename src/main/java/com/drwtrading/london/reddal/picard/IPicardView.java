package com.drwtrading.london.reddal.picard;

public interface IPicardView {

    public void picard(final String symbol, final String display, final String side, final String bpsThrough, final String opportunitySize,
            final String ccy, final String price, final String description, final String state, final boolean inAuction,
            final String long_price, final boolean isPlaySound);

    public void setCheckCrossed(final boolean checkCrossed);

    void setSound(final String fileName);
}
