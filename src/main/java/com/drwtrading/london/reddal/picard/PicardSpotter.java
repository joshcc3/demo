package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.LaserLine;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.data.MDForSymbol;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class PicardSpotter {

    private static final long RECHECK_PICARD_PERIOD_MILLIS = 500L;

    static final int FADE_TIMEOUT_MS = 1000;
    static final int DEAD_TIMEOUT_MS = 8000;

    private final IClock clock;
    private final IMDSubscriber bookSubscriber;
    private final Publisher<PicardRow> rowPublisher;

    private final DecimalFormat df;

    private final Map<String, PicardData> picardDatas;

    public PicardSpotter(final IClock clock, final IMDSubscriber bookSubscriber, final Publisher<PicardRow> rowPublisher) {

        this.clock = clock;
        this.bookSubscriber = bookSubscriber;
        this.rowPublisher = rowPublisher;

        this.df = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 0, 10);

        this.picardDatas = new HashMap<>();
    }

    public void setLaserLine(final LaserLine laserLine) {

        final PicardData picardData = picardDatas.get(laserLine.getSymbol());
        if (null == picardData) {
            final MDForSymbol mdForSymbol = new MDForSymbol(bookSubscriber, laserLine.getSymbol());
            final PicardData newPicardData = new PicardData(laserLine.getSymbol(), mdForSymbol);
            setLaserLine(newPicardData, laserLine);
            picardDatas.put(laserLine.getSymbol(), newPicardData);
        } else {
            setLaserLine(picardData, laserLine);
        }
    }

    private static void setLaserLine(final PicardData picardData, final LaserLine laserLine) {

        switch (laserLine.getType()) {
            case BID: {
                picardData.bidLaserLine = laserLine;
                break;
            }
            case ASK: {
                picardData.askLaserLine = laserLine;
                break;
            }
        }
    }

    public long checkAnyCrossed() {

        for (final PicardData picardData : picardDatas.values()) {
            checkCrossed(picardData);
        }
        return RECHECK_PICARD_PERIOD_MILLIS;
    }

    private void checkCrossed(final PicardData picardData) {

        final IBook<?> book = picardData.mdForSymbol.getBook();

        if (null != book) {

            final LaserLine bidLaserLine = picardData.bidLaserLine;
            final LaserLine askLaserLine = picardData.askLaserLine;

            final boolean isNewRow = null == picardData.previousRow;

            final String description;
            if (isNewRow) {
                description = "THEO";
            } else {
                description = picardData.previousRow.description;
            }

            final long nowMilliSinceUTC = clock.nowMilliUTC();

            final boolean isInAuction = BookMarketState.AUCTION == book.getStatus();
            final long bestBid;
            final long bestAsk;

            if (!book.isValid() || BookMarketState.CLOSED == book.getStatus()) {
                bestBid = Long.MIN_VALUE;
                bestAsk = Long.MAX_VALUE;
            } else if (isInAuction) {
                final IBookReferencePrice auctionIndicative = book.getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
                if (auctionIndicative.isValid()) {
                    bestBid = auctionIndicative.getPrice();
                    bestAsk = auctionIndicative.getPrice();
                } else {
                    bestBid = Long.MIN_VALUE;
                    bestAsk = Long.MAX_VALUE;
                }
            } else {
                final IBookLevel bestBidLevel = book.getBestBid();
                if (null == bestBidLevel) {
                    bestBid = Long.MIN_VALUE;
                } else {
                    bestBid = bestBidLevel.getPrice();
                }

                final IBookLevel bestAskLevel = book.getBestAsk();
                if (null == bestAskLevel) {
                    bestAsk = Long.MAX_VALUE;
                } else {
                    bestAsk = bestAskLevel.getPrice();
                }
            }

            if (null != bidLaserLine && bidLaserLine.isValid() && bestAsk <= bidLaserLine.getPrice()) {

                final String askPrice = df.format(bestAsk);
                final double bpsThrough = getBPSThrough(bidLaserLine.getPrice(), bestAsk);

                picardData.previousRow =
                        new PicardRow(nowMilliSinceUTC, book, BookSide.BID, bestAsk, askPrice, bpsThrough, PicardRowState.LIVE, description,
                                isInAuction, isNewRow);

                rowPublisher.publish(picardData.previousRow);

            } else if (null != askLaserLine && askLaserLine.isValid() && askLaserLine.getPrice() <= bestBid) {

                final String bidPrice = df.format(bestBid);
                final double bpsThrough = getBPSThrough(askLaserLine.getPrice(), bestBid);

                picardData.previousRow =
                        new PicardRow(nowMilliSinceUTC, book, BookSide.ASK, bestBid, bidPrice, bpsThrough, PicardRowState.LIVE, description,
                                isInAuction, isNewRow);

                rowPublisher.publish(picardData.previousRow);

            } else if (null != picardData.previousRow) {

                final long timeSinceLastUpdate = nowMilliSinceUTC - picardData.previousRow.milliSinceMidnight;

                if (DEAD_TIMEOUT_MS < timeSinceLastUpdate) {

                    final PicardRow row = new PicardRow(picardData.previousRow, PicardRowState.DEAD);
                    picardData.previousRow = null;
                    rowPublisher.publish(row);

                } else if (FADE_TIMEOUT_MS < timeSinceLastUpdate && PicardRowState.FADE != picardData.previousRow.state) {

                    picardData.previousRow = new PicardRow(picardData.previousRow, PicardRowState.FADE);
                    rowPublisher.publish(picardData.previousRow);
                }
            }
        }
    }

    private static double getBPSThrough(final long theoreticalValue, final long price) {

        final long divisor = Math.min(price, theoreticalValue);
        if (0 == divisor) {
            return 0d;
        } else {
            return 10000d * Math.abs(theoreticalValue - price) / divisor;
        }
    }
}
