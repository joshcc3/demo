package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.eeif.utils.marketData.book.IBookReferencePrice;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class PicardSpotter implements IPicardSpotter {

    private static final long RECHECK_PICARD_PERIOD_MILLIS = 500L;

    private static final int FADE_TIMEOUT_MS = 1000;
    private static final int DEAD_TIMEOUT_MS = 8000;

    private final IClock clock;
    private final IMDSubscriber bookSubscriber;
    private final Publisher<PicardRow> rowPublisher;

    private final DecimalFormat df;

    private final Map<String, PicardData> picardDatas;
    private final FXCalc<?> fxCalc;

    public PicardSpotter(final IClock clock, final IMDSubscriber bookSubscriber, final Publisher<PicardRow> rowPublisher,
            final FXCalc<?> fxCalc) {

        this.clock = clock;
        this.bookSubscriber = bookSubscriber;
        this.rowPublisher = rowPublisher;
        this.fxCalc = fxCalc;

        this.df = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 0, 10);

        this.picardDatas = new HashMap<>();
    }

    @Override
    public void setLaserLine(final LaserLineValue laserLine) {

        final PicardData picardData = picardDatas.get(laserLine.symbol);
        if (null == picardData) {
            final MDForSymbol mdForSymbol = bookSubscriber.subscribeForMD(laserLine.symbol, this);
            final PicardData newPicardData = new PicardData(laserLine.symbol, mdForSymbol);
            setLaserLine(newPicardData, laserLine);
            picardDatas.put(laserLine.symbol, newPicardData);
        } else {
            setLaserLine(picardData, laserLine);
        }
    }

    private static void setLaserLine(final PicardData picardData, final LaserLineValue laserLine) {

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

            final LaserLineValue bidLaserLine = picardData.bidLaserLine;
            final LaserLineValue askLaserLine = picardData.askLaserLine;

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

            if (null != bidLaserLine && bidLaserLine.isValid() && bestAsk <= bidLaserLine.getValue()) {

                final String askPrice = df.format(bestAsk / (double) Constants.NORMALISING_FACTOR);
                final double bpsThrough = getBPSThrough(bidLaserLine.getValue(), bestAsk);

                picardData.previousRow =
                        createPicardRow(book, book.getBestAsk(), BookSide.ASK, bidLaserLine, isNewRow, description, nowMilliSinceUTC,
                                isInAuction, bestAsk, askPrice, bpsThrough);

                rowPublisher.publish(picardData.previousRow);

            } else if (null != askLaserLine && askLaserLine.isValid() && askLaserLine.getValue() <= bestBid) {

                final String bidPrice = df.format(bestBid / (double) Constants.NORMALISING_FACTOR);
                final double bpsThrough = getBPSThrough(askLaserLine.getValue(), bestBid);
                picardData.previousRow =
                        createPicardRow(book, book.getBestBid(), BookSide.BID, askLaserLine, isNewRow, description, nowMilliSinceUTC,
                                isInAuction, bestBid, bidPrice, bpsThrough);

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

    private PicardRow createPicardRow(final IBook<?> book, final IBookLevel bestLevel, final BookSide side, final LaserLineValue laserLine,
            final boolean isNewRow, final String description, final long nowMilliSinceUTC, final boolean isInAuction, final long bestPrice,
            final String bestPricePrint, final double bpsThrough) {

        final double fx;
        final CCY opportunityCcy;
        if (fxCalc.isValid(book.getCCY(), CCY.EUR)) {
            opportunityCcy = CCY.EUR;
            fx = fxCalc.get(book.getCCY(), CCY.EUR, side);
        } else {
            opportunityCcy = book.getCCY();
            fx = 1;
        }

        final double opportunitySize;
        if (isInAuction) {
            final IBookReferencePrice refPriceData = book.getRefPriceData(ReferencePoint.AUCTION_INDICATIVE);
            if (refPriceData.isValid()) {
                opportunitySize =
                        opportunitySizeForLevel(laserLine.getValue(), refPriceData.getPrice(), refPriceData.getQty(), book.getWPV(), fx);
            } else {
                opportunitySize = 0;
            }
        } else {
            opportunitySize = calculateOpportunitySize(laserLine.getValue(), bestLevel, side, fx);
        }

        return new PicardRow(nowMilliSinceUTC, book.getSymbol(), book.getInstType(), opportunityCcy, side.getOppositeSide(), bestPrice,
                bestPricePrint, bpsThrough, opportunitySize, PicardRowState.LIVE, description, isInAuction, isNewRow);
    }

    private static double opportunitySizeForLevel(final long theoreticalValue, final long levelPrice, final long levelQty, final double wpv,
            final double fx) {
        return (Math.abs(theoreticalValue - levelPrice) * levelQty / (double) Constants.NORMALISING_FACTOR) * wpv * fx;
    }

    private static double calculateOpportunitySize(final long theoreticalValue, IBookLevel level, final BookSide side, final double fx) {
        double opportunitySize = 0;

        final int sign = side == BookSide.BID ? 1 : -1;
        while (level != null && sign * (level.getPrice() - theoreticalValue) > 0) {
            opportunitySize += opportunitySizeForLevel(theoreticalValue, level.getPrice(), level.getQty(), level.getBook().getWPV(), fx);
            level = level.next();
        }

        return opportunitySize;
    }

    static double getBPSThrough(final long theoreticalValue, final long price) {

        final long divisor = Math.min(price, theoreticalValue);
        if (0 == divisor) {
            return 0d;
        } else {
            return 10000d * Math.abs(theoreticalValue - price) / divisor;
        }
    }
}
