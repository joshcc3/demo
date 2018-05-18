package com.drwtrading.london.reddal;

import com.drwtrading.eeif.md.utils.L2DebugAdapter;
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
import com.drwtrading.london.eeif.utils.monitoring.ResourceIgnorer;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.eeif.utils.time.SystemClock;
import com.drwtrading.london.reddal.data.LaserLineType;
import com.drwtrading.london.reddal.data.LaserLineValue;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
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

    private final IMDSubscriber bookSubscriber = Mockito.mock(IMDSubscriber.class);
    @SuppressWarnings("unchecked")
    private final Publisher<PicardRow> picardPublisher = Mockito.mock(Publisher.class);

    private FXCalc<PicardFXCalcComponents> fxCalc;

    @BeforeMethod
    public void setUp() {
        Mockito.reset(bookSubscriber, picardPublisher);

        fxCalc = new FXCalc<>(new ResourceIgnorer<>(), PicardFXCalcComponents.FX_ERROR, MDSource.HOTSPOT_FX);
        fxCalc.setRate(CCY.EUR, CCY.USD, 1, true, 1.5, 1.5);
    }

    @Test
    public void picardSpotOpportunityMarketOpenTest() {

        final PicardSpotter picardSpotter = new PicardSpotter(new SystemClock(), bookSubscriber, picardPublisher, fxCalc);

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
        final PicardSpotter picardSpotter = new PicardSpotter(new SystemClock(), bookSubscriber, picardPublisher, fxCalc);

        setUpBook(bookSubscriber, CCY.USD, BookMarketState.CONTINUOUS);
        final LaserLineValue laserLine = getLaserLine(LaserLineType.BID, 100);

        picardSpotter.setLaserLine(laserLine);
        picardSpotter.checkAnyCrossed();

        Mockito.verify(picardPublisher, Mockito.never()).publish(Mockito.any());
    }

    @Test
    public void picardSpotOpportunityAuctionTest() {
        final PicardSpotter picardSpotter = new PicardSpotter(new SystemClock(), bookSubscriber, picardPublisher, fxCalc);

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

    private static void setUpBook(final IMDSubscriber bookSubscriber, final CCY ccy, final BookMarketState marketState) {
        final InstrumentID instrumentID = new InstrumentID("AAPL12345678", ccy, MIC.XCME);
        final LevelTwoBook book =
                new LevelTwoBook(new L2DebugAdapter(System.out, new BookLevelTwoMonitorAdaptor()), "AAPL", 1, instrumentID, InstType.FUTURE,
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

        final MDForSymbol mdForSymbol = new MDForSymbol("AAPL");
        Mockito.when(bookSubscriber.subscribeForMD(Mockito.any(), Mockito.any())).thenReturn(mdForSymbol);
        mdForSymbol.setBook(book);
    }

    private static LaserLineValue getLaserLine(final LaserLineType laserLineType, final int price) {

        final LaserLineValue laserLine = new LaserLineValue("AAPL", laserLineType);
        laserLine.setValue(price * Constants.NORMALISING_FACTOR);
        return laserLine;
    }
}
