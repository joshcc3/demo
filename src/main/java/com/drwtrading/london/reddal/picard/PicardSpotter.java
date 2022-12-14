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
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.IClock;
import com.drwtrading.london.reddal.data.DataUtils;
import com.drwtrading.london.reddal.data.LaserLine;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class PicardSpotter implements IPicardSpotter {

    private static final long RECHECK_PICARD_PERIOD_MILLIS = 500L;

    private static final int FADE_TIMEOUT_MS = 1000;
    private static final int DEAD_TIMEOUT_MS = 8000;

    private static final int MAX_DISTANCE_BPS = 10;

    private final IClock clock;
    private final IMDSubscriber bookSubscriber;
    private final Publisher<PicardRowWithInstID> rowPublisher;
    private final Publisher<LiquidityFinderData> laserDistancesPublisher;

    private final DecimalFormat df;

    private final Map<String, PicardData> picardDatas;
    private final FXCalc<?> fxCalc;

    public PicardSpotter(final IClock clock, final IMDSubscriber bookSubscriber, final Publisher<PicardRowWithInstID> rowPublisher,
            final Publisher<LiquidityFinderData> laserDistancesPublisher, final FXCalc<?> fxCalc) {

        this.clock = clock;
        this.bookSubscriber = bookSubscriber;
        this.rowPublisher = rowPublisher;
        this.laserDistancesPublisher = laserDistancesPublisher;
        this.fxCalc = fxCalc;

        this.df = NumberFormatUtil.getDF(NumberFormatUtil.SIMPLE, 0, 10);

        this.picardDatas = new HashMap<>();
    }

    @Override
    public void setLaserLine(final LaserLine laserLine) {

        final PicardData picardData = picardDatas.get(laserLine.symbol);
        if (null == picardData) {
            final PicardData newPicardData = new PicardData(laserLine.symbol);
            setLaserLine(newPicardData, laserLine);
            picardDatas.put(laserLine.symbol, newPicardData);
        } else {
            setLaserLine(picardData, laserLine);
        }
    }

    private void setLaserLine(final PicardData picardData, final LaserLine laserLine) {

        switch (laserLine.getType()) {
            case BID: {
                checkMDSubscription(picardData, laserLine.isValid());
                picardData.bidLaserLine = laserLine;
                break;
            }
            case ASK: {
                checkMDSubscription(picardData, laserLine.isValid());
                picardData.askLaserLine = laserLine;
                break;
            }
        }
    }

    private void checkMDSubscription(final PicardData picardData, final boolean isLaserLineValid) {

        if (null == picardData.mdForSymbol && isLaserLineValid) {
            picardData.mdForSymbol = bookSubscriber.subscribeForMD(picardData.symbol, this);
        }
    }

    public long checkAnyCrossed() {

        for (final PicardData picardData : picardDatas.values()) {
            checkCrossed(picardData);
        }
        return RECHECK_PICARD_PERIOD_MILLIS;
    }

    private void checkCrossed(final PicardData picardData) {

        final IBook<?> book = null == picardData.mdForSymbol ? null : picardData.mdForSymbol.getBook();

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

                    final PicardRowWithInstID row = new PicardRowWithInstID(picardData.previousRow, PicardRowState.DEAD);
                    picardData.previousRow = null;
                    rowPublisher.publish(row);

                } else if (FADE_TIMEOUT_MS < timeSinceLastUpdate && PicardRowState.FADE != picardData.previousRow.state) {

                    picardData.previousRow = new PicardRowWithInstID(picardData.previousRow, PicardRowState.FADE);
                    rowPublisher.publish(picardData.previousRow);
                }
            }

            if (InstType.FUTURE_SPREAD != book.getInstType()) {

                if (!isInAuction && null != bidLaserLine && bidLaserLine.isValid() && Long.MAX_VALUE != bestAsk) {

                    final double bpsFromTouch = getBPSAway(bidLaserLine.getValue(), bestAsk);
                    if (bpsFromTouch < MAX_DISTANCE_BPS) {

                        if (Constants.EPSILON < Math.abs(bpsFromTouch - picardData.bidLaserDistance.bpsFromTouch)) {
                            picardData.bidLaserDistance = new LiquidityFinderData(picardData.symbol, true, BookSide.BID, bpsFromTouch);
                            laserDistancesPublisher.publish(picardData.bidLaserDistance);
                        }
                    } else if (picardData.bidLaserDistance.isValid) {

                        picardData.bidLaserDistance = picardData.invalidBidLaserDistanceRow;
                        laserDistancesPublisher.publish(picardData.bidLaserDistance);
                    }

                } else if (picardData.bidLaserDistance.isValid) {

                    picardData.bidLaserDistance = picardData.invalidBidLaserDistanceRow;
                    laserDistancesPublisher.publish(picardData.bidLaserDistance);
                }

                if (!isInAuction && null != askLaserLine && askLaserLine.isValid() && Long.MAX_VALUE != bestBid) {

                    final double bpsFromTouch = getBPSAway(bestBid, askLaserLine.getValue());
                    if (bpsFromTouch < MAX_DISTANCE_BPS) {

                        if (Constants.EPSILON < Math.abs(bpsFromTouch - picardData.askLaserDistance.bpsFromTouch)) {
                            picardData.askLaserDistance = new LiquidityFinderData(picardData.symbol, true, BookSide.ASK, bpsFromTouch);
                            laserDistancesPublisher.publish(picardData.askLaserDistance);
                        }
                    } else if (picardData.askLaserDistance.isValid) {

                        picardData.askLaserDistance = picardData.invalidAskLaserDistanceRow;
                        laserDistancesPublisher.publish(picardData.askLaserDistance);
                    }
                } else if (picardData.askLaserDistance.isValid) {

                    picardData.askLaserDistance = picardData.invalidAskLaserDistanceRow;
                    laserDistancesPublisher.publish(picardData.askLaserDistance);
                }
            }
        }
    }

    private PicardRowWithInstID createPicardRow(final IBook<?> book, final IBookLevel bestLevel, final BookSide side,
            final LaserLine laserLine, final boolean isNewRow, final String description, final long nowMilliSinceUTC,
            final boolean isInAuction, final long bestPrice, final String bestPricePrint, final double bpsThrough) {

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

        return new PicardRowWithInstID(nowMilliSinceUTC, book.getSymbol(), book.getInstID(), book.getInstType(), opportunityCcy,
                side.getOppositeSide(), bestPrice, bestPricePrint, bpsThrough, opportunitySize, PicardRowState.LIVE, description,
                isInAuction, isNewRow);
    }

    private static double opportunitySizeForLevel(final long theoreticalValue, final long levelPrice, final long levelQty, final double wpv,
            final double fx) {

        return (Math.abs(theoreticalValue - levelPrice) * levelQty / (double) Constants.NORMALISING_FACTOR/ (double) Constants.NORMALISING_FACTOR) * wpv * fx;
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

    public static double getBPSThrough(final long theoreticalValue, final long price) {

        final long divisor = Math.min(price, theoreticalValue);
        if (0 == divisor) {
            return 0d;
        } else {
            return 10000d * Math.abs(theoreticalValue - price) / divisor;
        }
    }

    private static double getBPSAway(final long bid, final long ask) {

        final long divisor = Math.min(bid, ask);
        if (0 == divisor) {
            return 0d;
        } else if (0 < ask) {
            return 10_000d * (ask - bid) / divisor;
        } else {
            return 10_000d * (bid - ask) / divisor;
        }
    }
}
