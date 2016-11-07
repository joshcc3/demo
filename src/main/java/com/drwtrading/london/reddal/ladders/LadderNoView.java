package com.drwtrading.london.reddal.ladders;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

import java.util.Map;

public final class LadderNoView implements ILadderBoard {

    public static final LadderNoView SINGLETON = new LadderNoView();

    private LadderNoView() {
        // singleton
    }

    @Override
    public void switchedTo() {

    }

    @Override
    public void timedRefresh() {

    }

    @Override
    public void refresh(final String symbol) {

    }

    @Override
    public void setTradingBoxQty(final int value) {

    }

    @Override
    public boolean setPersistencePreference(final String label, final String value) {
        return true;
    }

    @Override
    public boolean canMoveTowardsCenter() {
        return false;
    }

    @Override
    public void setCenteredPrice(final long newCenterPrice) {

    }

    @Override
    public long getCenteredPrice() {
        return 0;
    }

    @Override
    public void center() {

    }

    @Override
    public boolean moveTowardsCenter() {
        return true;
    }

    @Override
    public void setBestAskCenter() {

    }

    @Override
    public void setBestBidCenter() {

    }

    @Override
    public void scrollUp() {

    }

    @Override
    public void scrollDown() {

    }

    @Override
    public void pageUp() {

    }

    @Override
    public void pageDown() {

    }

    @Override
    public void onClick(final ClientSpeedState clientSpeedState, final String label, final String button, final Map<String, String> data) {

    }

    @Override
    public void cancelAllForSide(final BookSide side) {

    }
}
