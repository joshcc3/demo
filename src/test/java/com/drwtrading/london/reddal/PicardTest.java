package com.drwtrading.london.reddal;

import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.BookLevelTwoMonitorAdaptor;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.ReferencePoint;
import com.drwtrading.london.eeif.utils.marketData.book.impl.levelTwo.LevelTwoBook;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.SingleBandTickTable;
import com.drwtrading.london.eeif.utils.marketData.fx.FXCalc;
import com.drwtrading.london.eeif.utils.monitoring.IgnoredFuseBox;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.eeif.utils.time.SystemClock;
import com.drwtrading.london.reddal.data.LaserLineType;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.picard.LiquidityFinderData;
import com.drwtrading.london.reddal.picard.PicardFXCalcComponents;
import com.drwtrading.london.reddal.picard.PicardRow;
import com.drwtrading.london.reddal.picard.PicardSpotter;
import org.jetlang.channels.Publisher;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class PicardTest {

    private static final String SYMBOL = "AAPL UF";

    private final IMDSubscriber bookSubscriber = Mockito.mock(IMDSubscriber.class);
    @SuppressWarnings("unchecked")
    private final Publisher<PicardRow> picardPublisher = Mockito.mock(Publisher.class);
    @SuppressWarnings("unchecked")
    private final Publisher<LiquidityFinderData> laserDistancesPublisher = Mockito.mock(Publisher.class);

    private FXCalc<PicardFXCalcComponents> fxCalc;

    @BeforeMethod
    public void setUp() {
        Mockito.reset(bookSubscriber, picardPublisher, laserDistancesPublisher);

        fxCalc = new FXCalc<>(new IgnoredFuseBox<>(), PicardFXCalcComponents.FX_ERROR, MDSource.HOTSPOT_FX);
        fxCalc.setRate(CCY.EUR, CCY.USD, 1, true, 1.5, 1.5);
    }

    @Test
    public void picardSpotOpportunityMarketOpenTest() {

        final PicardSpotter picardSpotter =
                new PicardSpotter(new SystemClock(), bookSubscriber, picardPublisher, laserDistancesPublisher, fxCalc);

        setUpBook(bookSubscriber, CCY.USD, BookMarketState.CONTINUOUS);
        final LaserLineValue laserLine = getLaserLine(LaserLineType.BID, 103);

        picardSpotter.setLaserLine(laserLine);
        picardSpotter.checkAnyCrossed();

        final ArgumentCaptor<PicardRow> picardRowCapture = ArgumentCaptor.forClass(PicardRow.class);
        Mockito.verify(picardPublisher).publish(picardRowCapture.capture());

        final PicardRow picardRow = picardRowCapture.getValue();
        Assert.assertEquals(picardRow.ccy, CCY.EUR, "Did not convert to EUR even though FX was present.");
        Assert.assertEquals(picardRow.opportunitySize, (2 + 5) * fxCalc.getMid(CCY.USD, CCY.EUR),
                "Did not calculate opportunity size correctly");

    }

    @Test
    public void picardDoesNotSpotNonExistentOpportunityMarketOpenTest() {

        final PicardSpotter picardSpotter =
                new PicardSpotter(new SystemClock(), bookSubscriber, picardPublisher, laserDistancesPublisher, fxCalc);

        setUpBook(bookSubscriber, CCY.USD, BookMarketState.CONTINUOUS);
        final LaserLineValue laserLine = getLaserLine(LaserLineType.BID, 100);

        picardSpotter.setLaserLine(laserLine);
        picardSpotter.checkAnyCrossed();

        Mockito.verify(picardPublisher, Mockito.never()).publish(Mockito.any());
    }

    @Test
    public void picardSpotOpportunityAuctionTest() {

        final PicardSpotter picardSpotter =
                new PicardSpotter(new SystemClock(), bookSubscriber, picardPublisher, laserDistancesPublisher, fxCalc);

        setUpBook(bookSubscriber, CCY.USD, BookMarketState.AUCTION);
        final LaserLineValue laserLine = getLaserLine(LaserLineType.BID, 103);

        picardSpotter.setLaserLine(laserLine);
        picardSpotter.checkAnyCrossed();

        final ArgumentCaptor<PicardRow> picardRowCapture = ArgumentCaptor.forClass(PicardRow.class);
        Mockito.verify(picardPublisher).publish(picardRowCapture.capture());

        final PicardRow picardRow = picardRowCapture.getValue();
        Assert.assertEquals(picardRow.ccy, CCY.EUR, "Did not convert to EUR even though FX was present.");
        Assert.assertEquals(picardRow.opportunitySize, 2 * fxCalc.getMid(CCY.USD, CCY.EUR), "Did not calculate opportunity size correctly");
    }

    @Test
    public void bidLaserDistanceTest() {

        final PicardSpotter picardSpotter =
                new PicardSpotter(new SystemClock(), bookSubscriber, picardPublisher, laserDistancesPublisher, fxCalc);

        setUpBook(bookSubscriber, CCY.USD, BookMarketState.CONTINUOUS);
        final LaserLineValue laserLine = getLaserLine(LaserLineType.BID, 100.909181736d);
        picardSpotter.setLaserLine(laserLine);

        picardSpotter.checkAnyCrossed();

        final ArgumentCaptor<LiquidityFinderData> laserDistanceCapture = ArgumentCaptor.forClass(LiquidityFinderData.class);
        Mockito.verify(laserDistancesPublisher).publish(laserDistanceCapture.capture());

        final LiquidityFinderData laserDistance = laserDistanceCapture.getValue();
        Assert.assertEquals(laserDistance.symbol, SYMBOL, "Symbol.");
        Assert.assertTrue(laserDistance.isValid, "Is Valid.");
        Assert.assertEquals(laserDistance.side, BookSide.BID, "Side.");
        Assert.assertEquals(laserDistance.bpsFromTouch, 9, 0.0001d, "BPS to touch.");

        picardSpotter.checkAnyCrossed();

        Mockito.verifyNoMoreInteractions(laserDistancesPublisher);

        final LaserLineValue laserLineTwo = getLaserLine(LaserLineType.BID, 101.0505d);
        picardSpotter.setLaserLine(laserLineTwo);

        picardSpotter.checkAnyCrossed();

        Mockito.verify(laserDistancesPublisher, Mockito.times(2)).publish(laserDistanceCapture.capture());

        final LiquidityFinderData laserDistanceTwo = laserDistanceCapture.getValue();
        Assert.assertEquals(laserDistanceTwo.symbol, SYMBOL, "Symbol.");
        Assert.assertTrue(laserDistanceTwo.isValid, "Is Valid.");
        Assert.assertEquals(laserDistanceTwo.side, BookSide.BID, "Side.");
        Assert.assertEquals(laserDistanceTwo.bpsFromTouch, -5, 0.0001d, "BPS to touch.");

        final LaserLineValue laserLineThree = getLaserLine(LaserLineType.BID, 80);
        picardSpotter.setLaserLine(laserLineThree);

        picardSpotter.checkAnyCrossed();

        Mockito.verify(laserDistancesPublisher, Mockito.times(3)).publish(laserDistanceCapture.capture());

        final LiquidityFinderData laserDistanceThree = laserDistanceCapture.getValue();
        Assert.assertEquals(laserDistanceThree.symbol, SYMBOL, "Symbol.");
        Assert.assertFalse(laserDistanceThree.isValid, "Is Valid.");
        Assert.assertEquals(laserDistanceThree.side, BookSide.BID, "Side.");
    }

    @Test
    public void askLaserDistanceTest() {

        final PicardSpotter picardSpotter =
                new PicardSpotter(new SystemClock(), bookSubscriber, picardPublisher, laserDistancesPublisher, fxCalc);

        setUpBook(bookSubscriber, CCY.USD, BookMarketState.CONTINUOUS);
        final LaserLineValue laserLine = getLaserLine(LaserLineType.ASK, 100.09d);
        picardSpotter.setLaserLine(laserLine);

        picardSpotter.checkAnyCrossed();

        final ArgumentCaptor<LiquidityFinderData> laserDistanceCapture = ArgumentCaptor.forClass(LiquidityFinderData.class);
        Mockito.verify(laserDistancesPublisher).publish(laserDistanceCapture.capture());

        final LiquidityFinderData laserDistance = laserDistanceCapture.getValue();
        Assert.assertEquals(laserDistance.symbol, SYMBOL, "Symbol.");
        Assert.assertTrue(laserDistance.isValid, "Is Valid.");
        Assert.assertEquals(laserDistance.side, BookSide.ASK, "Side.");
        Assert.assertEquals(laserDistance.bpsFromTouch, 9, 0.0001d, "BPS to touch.");

        picardSpotter.checkAnyCrossed();

        Mockito.verifyNoMoreInteractions(laserDistancesPublisher);

        final LaserLineValue laserLineTwo = getLaserLine(LaserLineType.ASK, 80d);
        picardSpotter.setLaserLine(laserLineTwo);

        picardSpotter.checkAnyCrossed();

        Mockito.verify(laserDistancesPublisher, Mockito.times(2)).publish(laserDistanceCapture.capture());

        final LiquidityFinderData laserDistanceTwo = laserDistanceCapture.getValue();
        Assert.assertEquals(laserDistanceTwo.symbol, SYMBOL, "Symbol.");
        Assert.assertTrue(laserDistanceTwo.isValid, "Is Valid.");
        Assert.assertEquals(laserDistanceTwo.side, BookSide.ASK, "Side.");
        Assert.assertEquals(laserDistanceTwo.bpsFromTouch, -2500, 0.0001d, "BPS to touch.");

        final LaserLineValue laserLineThree = getLaserLine(LaserLineType.ASK, 120);
        picardSpotter.setLaserLine(laserLineThree);

        picardSpotter.checkAnyCrossed();

        Mockito.verify(laserDistancesPublisher, Mockito.times(3)).publish(laserDistanceCapture.capture());

        final LiquidityFinderData laserDistanceThree = laserDistanceCapture.getValue();
        Assert.assertEquals(laserDistanceThree.symbol, SYMBOL, "Symbol.");
        Assert.assertFalse(laserDistanceThree.isValid, "Is Valid.");
        Assert.assertEquals(laserDistanceThree.side, BookSide.ASK, "Side.");
    }

    private static void setUpBook(final IMDSubscriber bookSubscriber, final CCY ccy, final BookMarketState marketState) {

        final InstrumentID instrumentID = new InstrumentID("AAPL12345678", ccy, MIC.XCME);
        final LevelTwoBook book = new LevelTwoBook(new BookLevelTwoMonitorAdaptor(), SYMBOL, 1, instrumentID, InstType.FUTURE,
                new SingleBandTickTable(Constants.NORMALISING_FACTOR), MDSource.LSE, 1, 100);
        book.setLevel(BookSide.ASK, 103 * Constants.NORMALISING_FACTOR, 10);
        book.setLevel(BookSide.ASK, 102 * Constants.NORMALISING_FACTOR, 5);
        book.setLevel(BookSide.ASK, 101 * Constants.NORMALISING_FACTOR, 1);
        book.setLevel(BookSide.BID, 100 * Constants.NORMALISING_FACTOR, 1);
        book.setLevel(BookSide.BID, 99 * Constants.NORMALISING_FACTOR, 5);
        book.setLevel(BookSide.BID, 98 * Constants.NORMALISING_FACTOR, 10);

        if (marketState == BookMarketState.AUCTION) {
            book.referencePrice(ReferencePoint.AUCTION_INDICATIVE, 1, 101 * Constants.NORMALISING_FACTOR, 1);
        }

        book.setStatus(marketState);
        book.setValidity(true);

        final MDForSymbol mdForSymbol = new MDForSymbol(SYMBOL);
        Mockito.when(bookSubscriber.subscribeForMD(Mockito.any(), Mockito.any())).thenReturn(mdForSymbol);
        mdForSymbol.setBook(book);
    }

    private static LaserLineValue getLaserLine(final LaserLineType laserLineType, final double price) {

        final LaserLineValue laserLine = new LaserLineValue(SYMBOL, laserLineType);
        laserLine.setValue((long) (price * Constants.NORMALISING_FACTOR));
        return laserLine;
    }
}
