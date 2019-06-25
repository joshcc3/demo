package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

import java.util.Map;

public interface ILadderBoard {

    public void switchedTo();

    public void timedRefresh();

    public void refresh(final String symbol);

    public void setTradingBoxQty(final int qty);

    public void setStackTickSize(final double tickSize);

    public void setStackAlignmentTickToBPS(final double stackAlignmentTickToBPS);

    public void setStackTickSizeToMatchQuote();

    public boolean setPersistencePreference(final String label, final String value);

    public boolean canMoveTowardsCenter();

    public void setCenteredPrice(final long newCenterPrice);

    public long getCenteredPrice();

    public void center();

    /**
     * @return true if request honoured.
     */
    public boolean moveTowardsCenter();

    public void setBestAskCenter();

    public void setBestBidCenter();

    public void scrollUp();

    public void scrollDown();

    public void pageUp();

    public void pageDown();

    public void onClick(final ClientSpeedState clientSpeedState, final String label, final String button, final Map<String, String> data);

    public void cancelAllForSide(final BookSide side);

    public void zoomIn();

    public void zoomOut();
}
