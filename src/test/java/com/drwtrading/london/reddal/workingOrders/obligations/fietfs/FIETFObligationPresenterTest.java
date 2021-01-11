package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.io.ISelectIORunnable;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.websockets.WebSocketClient;
import com.drwtrading.websockets.WebSocketInboundData;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.Calendar;

public class FIETFObligationPresenterTest {

    public static final User USER = User.CMILLER;
    public static final boolean ACTIVE = true;
    public static final String SYMBOL = "VODFMZ0015_PGY";

    private static final SourcedWorkingOrder BID =
            new SourcedWorkingOrder("", new WorkingOrder(1, 1, 1, SYMBOL, "", BookSide.BID, AlgoType.MANUAL, OrderType.LIMIT, 1, 1, 1, 1));
    private static final SourcedWorkingOrder ASK =
            new SourcedWorkingOrder("", new WorkingOrder(2, 1, 1, SYMBOL, "", BookSide.ASK, AlgoType.MANUAL, OrderType.LIMIT, 1, 10, 1, 1));

    private final SelectIO selectIO = Mockito.mock(SelectIO.class);
    private final UILogger webLog = Mockito.mock(UILogger.class);
    private final NibblerTransportOrderEntry nibblerHandler = Mockito.mock(NibblerTransportOrderEntry.class);
    private final WebSocketInboundData inboundData = Mockito.mock(WebSocketInboundData.class);
    private final WebSocketClient webClient = Mockito.mock(WebSocketClient.class);
    private final ArgumentCaptor<ISelectIORunnable> checkObligations = ArgumentCaptor.forClass(ISelectIORunnable.class);

    @Mock
    private IFuseBox<ReddalComponents> monitor;

    private Calendar cal;

    @BeforeMethod
    public void reset() {
        cal = DateTimeUtil.getCalendar();
        cal.setTimeZone(DateTimeUtil.LONDON_TIME_ZONE);

        MockitoAnnotations.initMocks(this);
        Mockito.reset(selectIO, webLog, nibblerHandler, inboundData, webClient);
        Mockito.doReturn(webClient).when(inboundData).getClient();
        Mockito.doReturn(USER.username).when(webClient).getUserName();
        Mockito.when(selectIO.getMillisAtMidnightUTC()).thenReturn(DateTimeUtil.getMillisAtMidnight());
    }

    @Test
    public void correctStartingObligationPercentageTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(ACTIVE, selectIO, monitor);
        presenter.start();
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setWorkingOrder(BID);
        presenter.setWorkingOrder(ASK);
        checkObligations.getValue().run();

        setTimeFromMidnight(8, 30);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.never()).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
    }

    @Test
    public void fuseBlownAtCorrectPercentageStartingOnTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(ACTIVE, selectIO, monitor);
        presenter.start();
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setWorkingOrder(BID);
        presenter.setWorkingOrder(ASK);
        checkObligations.getValue().run();

        setTimeFromMidnight(9, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
    }

    @Test
    public void fuseStaysBlownUtilRecoveryTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(ACTIVE, selectIO, monitor);
        presenter.start();
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setWorkingOrder(BID);
        presenter.setWorkingOrder(ASK);
        checkObligations.getValue().run();

        setTimeFromMidnight(9, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        setTimeFromMidnight(9, 10);
        presenter.deleteWorkingOrder(BID);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        setTimeFromMidnight(11, 10);
        presenter.setWorkingOrder(BID);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        setTimeFromMidnight(12, 10);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(2)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);
    }

    @Test
    public void fuseBlownAtCorrectPercentageStartingOffTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(ACTIVE, selectIO, monitor);
        presenter.start();
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setWorkingOrder(BID);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.never()).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        setTimeFromMidnight(9, 0);
        presenter.setWorkingOrder(ASK);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.never()).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        setTimeFromMidnight(11, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);
    }

    @Test
    public void fuseRecoversWithPercentageRecoveringTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(ACTIVE, selectIO, monitor);
        presenter.start();
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setWorkingOrder(BID);
        presenter.setWorkingOrder(ASK);

        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.never()).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        // The formula is  timeOn / (totalTimeRunning + (half of remaining time in a day))
        // After 1 hour of being "on", we have 1 / (1 + (16.5 - 8 - 1) / 2) = 1/4.25 = 24%
        setTimeFromMidnight(9, 0);
        presenter.deleteWorkingOrder(ASK);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any());
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        // After 1 hour of being "on" and 1 hour "off", we have 1 / (2 + (16.5 - 8 - 2) / 2) = 1/5.25 = 19%
        setTimeFromMidnight(10, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any());
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        // After 1 hour of being "on" and 2 hours "off", we have 1 / (3 + (16.5 - 8 - 3) / 2) = 1/5.75 = 17%
        setTimeFromMidnight(11, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any());
        Mockito.verify(monitor, Mockito.times(3)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);
    }

    private void setTimeFromMidnight(final int hours, final int minutes) {
        DateTimeUtil.setToTimeOfDay(cal, hours, minutes, 0, 0);
        Mockito.when(selectIO.getMillisSinceMidnightUTC()).thenReturn(cal.getTimeInMillis() - DateTimeUtil.getMillisAtMidnight());
    }
}