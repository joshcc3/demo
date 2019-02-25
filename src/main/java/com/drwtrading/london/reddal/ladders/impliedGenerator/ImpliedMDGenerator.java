package com.drwtrading.london.reddal.ladders.impliedGenerator;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.shredders.ShredderPresenter;

class ImpliedMDGenerator {

    private final IClock clock;
    private final FutureConstant future;

    private final LadderPresenter ladderPresenter;
    private final ShredderPresenter shredderPresenter;

    private final SpreadnoughtTheo spreadnoughtTheo;

    private IBook<?> frontBook;
    private IBook<?> backBook;

    ImpliedMDGenerator(final IClock clock, final String symbol, final FutureConstant future, final LadderPresenter ladderPresenter,
            final ShredderPresenter shredderPresenter) {

        this.clock = clock;
        this.future = future;

        this.ladderPresenter = ladderPresenter;
        this.shredderPresenter = shredderPresenter;

        this.spreadnoughtTheo = new SpreadnoughtTheo(0, symbol);

        this.frontBook = null;
        this.backBook = null;

        updateTheo();
    }

    void frontMonthUpdated(final IBook<?> book) {

        this.frontBook = book;
        updateTheo();
    }

    void backMonthUpdated(final IBook<?> book) {

        this.backBook = book;
        updateTheo();
    }

    private void updateTheo() {

        final long nanoSinceMidnight = clock.getReferenceNanoSinceMidnightUTC();

        if (isBookValid(frontBook) && isBookValid(backBook)) {

            final boolean isBidValid;
            final long bidValue;

            final boolean isAskValid;
            final long askValue;

            final IBookLevel frontBid = frontBook.getBestBid();
            final IBookLevel frontAsk = frontBook.getBestAsk();

            final IBookLevel backBid = backBook.getBestBid();
            final IBookLevel backAsk = backBook.getBestAsk();

            if (future.isReverseSpread) {

                if (null == backBid || null == frontAsk) {
                    isBidValid = false;
                    bidValue = 0L;
                } else {
                    isBidValid = true;
                    bidValue = backBid.getPrice() - frontAsk.getPrice();
                }

                if (null == backAsk || null == frontBid) {
                    isAskValid = false;
                    askValue = 0L;
                } else {
                    isAskValid = true;
                    askValue = backAsk.getPrice() - frontBid.getPrice();
                }
            } else {

                if (null == frontBid || null == backAsk) {
                    isBidValid = false;
                    bidValue = 0L;
                } else {
                    isBidValid = true;
                    bidValue = frontBid.getPrice() - backAsk.getPrice();
                }

                if (null == backBid || null == frontAsk) {
                    isAskValid = false;
                    askValue = 0L;
                } else {
                    isAskValid = true;
                    askValue = frontAsk.getPrice() - backBid.getPrice();
                }
            }

            this.spreadnoughtTheo.set(nanoSinceMidnight, isBidValid, bidValue, isAskValid, askValue);

        } else {

            spreadnoughtTheo.set(nanoSinceMidnight, false, 0L, false, 0L);
        }

        ladderPresenter.setSpreadnoughtTheo(spreadnoughtTheo);
        shredderPresenter.setSpreadnoughtTheo(spreadnoughtTheo);
    }

    private static boolean isBookValid(final IBook<?> book) {

        return null != book && book.isValid() && null != book.getBestBid() && null != book.getBestAsk();
    }
}
