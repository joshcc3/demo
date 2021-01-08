package com.drwtrading.london.reddal.workingOrders.obligations.fietfs;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.io.ISelectIORunnable;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ReddalComponents;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.util.UILogger;
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
    public static final String APP = "LADDERS_GM_TEST";
    public static final String NIBBLER = "TEST_XETRA_NIBBLER";
    public static final String SYMBOL = "VOD LN";

    private static final QuotingState ON = new QuotingState(1, 1, SYMBOL, true, "OK");
    private static final QuotingState OFF = new QuotingState(1, 1, SYMBOL, true, SYMBOL);

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
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(APP, selectIO, monitor, webLog);
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, ON);
        checkObligations.getValue().run();

        setTimeFromMidnight(8, 30);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.never()).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
    }

    @Test
    public void fuseBlownAtCorrectPercentageStartingOnTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(APP, selectIO, monitor, webLog);
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, ON);
        checkObligations.getValue().run();

        setTimeFromMidnight(9, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
    }

    @Test
    public void fuseStaysBlownUtilRecoveryTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(APP, selectIO, monitor, webLog);
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, ON);
        checkObligations.getValue().run();

        presenter.setQuotingState(NIBBLER, ON);
        setTimeFromMidnight(9, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        presenter.setQuotingState(NIBBLER, ON);
        setTimeFromMidnight(9, 10);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        presenter.setQuotingState(NIBBLER, OFF);
        setTimeFromMidnight(11, 10);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        presenter.setQuotingState(NIBBLER, ON);
        setTimeFromMidnight(12, 10);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(2)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);
    }

    @Test
    public void fuseBlownAtCorrectPercentageStartingOffTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(APP, selectIO, monitor, webLog);
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, OFF);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.never()).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        setTimeFromMidnight(9, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.never()).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        presenter.setQuotingState(NIBBLER, ON);
        setTimeFromMidnight(11, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);
    }

    @Test
    public void fuseRecoversWithPercentageRecoveringTest() {
        setTimeFromMidnight(8, 0);
        final FIETFObligationPresenter presenter = new FIETFObligationPresenter(APP, selectIO, monitor, webLog);
        Mockito.verify(selectIO).addDelayedAction(Mockito.anyLong(), checkObligations.capture());

        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, ON);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.never()).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any(String.class));
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        // The formula is  timeOn / (totalTimeRunning + (half of remaining time in a day))
        // After 1 hour of being "on", we have 1 / (1 + (16.5 - 8 - 1) / 2) = 1/4.25 = 24%
        setTimeFromMidnight(9, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any());
        Mockito.verify(monitor, Mockito.times(1)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        // After 1 hour of being "on" and 1 hour "off", we have 1 / (2 + (16.5 - 8 - 2) / 2) = 1/5.25 = 19%
        presenter.setQuotingState(NIBBLER, OFF);
        setTimeFromMidnight(10, 0);
        checkObligations.getValue().run();

        Mockito.verify(monitor, Mockito.times(1)).logError(Mockito.eq(ReddalComponents.INVERSE_OBLIGATIONS), Mockito.any());
        Mockito.verify(monitor, Mockito.times(2)).setOK(ReddalComponents.INVERSE_OBLIGATIONS);

        // After 1 hour of being "on" and 2 hours "off", we have 1 / (3 + (16.5 - 8 - 3) / 2) = 1/5.75 = 17%
        presenter.setQuotingState(NIBBLER, OFF);
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