package com.drwtrading.london.reddal.autopull.marketNumbers;

import com.drwtrading.london.eeif.utils.io.ISelectIORunnable;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.NibblerTransportOrderEntry;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.StopAllForMarketNumberCmd;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.photons.eeif.configuration.MarketNumbers;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.channels.Publisher;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class MarketNumberPresenterTest {

    private final SelectIO selectIO = Mockito.mock(SelectIO.class);
    private final UILogger uiLogger = Mockito.mock(UILogger.class);
    @SuppressWarnings("unchecked")
    private final Publisher<IOrderCmd> cmdsPublisher = Mockito.mock(Publisher.class);

    private final NibblerTransportOrderEntry cmdHandler = Mockito.mock(NibblerTransportOrderEntry.class);

    @BeforeMethod
    public void setup() {

        Mockito.reset(selectIO, uiLogger, cmdsPublisher, cmdHandler);
    }

    @Test
    public void basicTest() {

        final MarketNumbers marketNumber = new MarketNumbers(-1, "Test", "today");
        final WebSocketInboundData data = Mockito.mock(WebSocketInboundData.class);

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);

        Mockito.verifyNoMoreInteractions(selectIO, uiLogger, cmdsPublisher);

        presenter.setMarketNumber(marketNumber);

        Mockito.verify(selectIO).nowMilliUTC();
        Mockito.verify(selectIO).getMillisAtMidnightUTC();
        Mockito.verifyNoMoreInteractions(selectIO, uiLogger, cmdsPublisher);

        presenter.ack(data);

        Mockito.verify(uiLogger).write(MarketNumberPresenter.class.getSimpleName(), data);
    }

    @Test
    public void newMarketNumberAddsCallbackTest() {

        final int numberTime = 70_000;
        final long callbackTime = numberTime - 60_000;

        final MarketNumbers marketNumber = new MarketNumbers(numberTime, "Test", "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);

        presenter.setMarketNumber(marketNumber);

        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.eq(callbackTime), Mockito.any(ISelectIORunnable.class));
    }

    @Test
    public void earlierMarketNumberTest() {

        final int numberTimeOne = 70_000;
        final int numberTimeTwo = numberTimeOne - 5_000;
        final long callbackTimeTwo = numberTimeOne - 60_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTimeOne, "Test", "today");
        final MarketNumbers marketNumberTwo = new MarketNumbers(numberTimeTwo, "Test", "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);

        presenter.setMarketNumber(marketNumberOne);
        presenter.setMarketNumber(marketNumberTwo);

        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.eq(callbackTimeTwo), Mockito.any(ISelectIORunnable.class));
    }

    @Test
    public void laterMarketNumberNoChangeTest() {

        final int numberTimeOne = 70_000;
        final int numberTimeTwo = numberTimeOne + 25_000;
        final int additionalDelayMillis = numberTimeTwo - 60_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTimeOne, "Test", "today");
        final MarketNumbers marketNumberTwo = new MarketNumbers(numberTimeTwo, "Test", "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);

        presenter.setMarketNumber(marketNumberOne);
        presenter.setMarketNumber(marketNumberTwo);

        final ArgumentCaptor<ISelectIORunnable> runnableArgumentCapture = ArgumentCaptor.forClass(ISelectIORunnable.class);
        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.anyLong(), runnableArgumentCapture.capture());

        final ISelectIORunnable capturedRunnable = runnableArgumentCapture.getValue();

        final long nextRun = capturedRunnable.run();

        Assert.assertEquals(nextRun, additionalDelayMillis, "Additional delay.");
    }

    @Test
    public void marketNumberCallbackTest() {

        final String reason = "Test REASON.";
        final int numberTime = 70_000;
        final long startWarnDelay = numberTime - 60_000;
        final long warningTime = numberTime - startWarnDelay;
        final long startFireTimeDelay = numberTime - warningTime - 3_999;
        final long fireTime = numberTime - 1_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTime, reason, "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);

        presenter.setMarketNumber(marketNumberOne);

        final ArgumentCaptor<ISelectIORunnable> runnableArgumentCapture = ArgumentCaptor.forClass(ISelectIORunnable.class);
        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.anyLong(), runnableArgumentCapture.capture());

        final ISelectIORunnable capturedRunnable = runnableArgumentCapture.getValue();

        final long nextRunOne = capturedRunnable.run();

        Assert.assertEquals(nextRunOne, startWarnDelay, "Until warning delay.");

        Mockito.verifyNoMoreInteractions(cmdsPublisher);

        Mockito.doReturn(warningTime).when(selectIO).nowMilliUTC();

        final long nextRunTwo = capturedRunnable.run();

        Assert.assertEquals(nextRunTwo, startFireTimeDelay, "Until fire delay.");

        Mockito.verifyNoMoreInteractions(cmdsPublisher);

        Mockito.doReturn(fireTime).when(selectIO).nowMilliUTC();

        final long nextRunThree = capturedRunnable.run();

        Assert.assertEquals(nextRunThree, -1, "After fire delay..");

        final ArgumentCaptor<StopAllForMarketNumberCmd> cmdCapture = ArgumentCaptor.forClass(StopAllForMarketNumberCmd.class);
        Mockito.verify(cmdsPublisher).publish(cmdCapture.capture());

        final StopAllForMarketNumberCmd cmd = cmdCapture.getValue();
        cmd.execute(cmdHandler);

        Mockito.verify(cmdHandler).stopAllForMarketNumber(Mockito.eq(false), Mockito.eq(reason));

        Mockito.verifyNoMoreInteractions(cmdsPublisher, cmdHandler);
    }

    @Test
    public void acknowledgedMarketNumberCallbackTest() {

        final String reason = "Test REASON.";
        final int numberTime = 70_000;
        final long startWarnDelay = numberTime - 60_000;
        final long warningTime = numberTime - startWarnDelay;
        final long startFireTimeDelay = numberTime - warningTime - 3_999;
        final long fireTime = numberTime - 1_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTime, reason, "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);

        presenter.setMarketNumber(marketNumberOne);

        final ArgumentCaptor<ISelectIORunnable> runnableArgumentCapture = ArgumentCaptor.forClass(ISelectIORunnable.class);
        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.anyLong(), runnableArgumentCapture.capture());

        final ISelectIORunnable capturedRunnable = runnableArgumentCapture.getValue();

        final long nextRunOne = capturedRunnable.run();

        Assert.assertEquals(nextRunOne, startWarnDelay, "Until warning delay.");

        Mockito.verifyNoMoreInteractions(cmdsPublisher);
        Mockito.doReturn(warningTime).when(selectIO).nowMilliUTC();

        final long nextRunTwo = capturedRunnable.run();

        Assert.assertEquals(nextRunTwo, startFireTimeDelay, "Until fire delay.");

        presenter.ack(null);

        Mockito.verifyNoMoreInteractions(cmdsPublisher);
        Mockito.doReturn(fireTime).when(selectIO).nowMilliUTC();

        final long nextRunThree = capturedRunnable.run();

        Assert.assertEquals(nextRunThree, -1, "After fire delay..");

        final ArgumentCaptor<StopAllForMarketNumberCmd> cmdCapture = ArgumentCaptor.forClass(StopAllForMarketNumberCmd.class);
        Mockito.verify(cmdsPublisher).publish(cmdCapture.capture());

        final StopAllForMarketNumberCmd cmd = cmdCapture.getValue();
        cmd.execute(cmdHandler);

        Mockito.verify(cmdHandler).stopAllForMarketNumber(Mockito.eq(true), Mockito.eq(reason));
        Mockito.verifyNoMoreInteractions(cmdsPublisher, cmdHandler);
    }

    @Test
    public void acknowledgingTestTest() {

        final String reason = "Test REASON.";
        final int numberTime = 70_000;
        final long startWarnDelay = numberTime - 60_000;
        final long warningTime = numberTime - startWarnDelay;
        final long startFireTimeDelay = numberTime - warningTime - 3_999;
        final long fireTime = numberTime - 1_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTime, reason, "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);

        presenter.setMarketNumber(marketNumberOne);

        final ArgumentCaptor<ISelectIORunnable> runnableArgumentCapture = ArgumentCaptor.forClass(ISelectIORunnable.class);
        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.anyLong(), runnableArgumentCapture.capture());
        final ISelectIORunnable capturedRunnable = runnableArgumentCapture.getValue();

        final long nextRunOne = capturedRunnable.run();

        Assert.assertEquals(nextRunOne, startWarnDelay, "Until warning delay.");

        Mockito.verifyNoMoreInteractions(cmdsPublisher);
        Mockito.doReturn(warningTime).when(selectIO).nowMilliUTC();

        final long nextRunTwo = capturedRunnable.run();

        Assert.assertEquals(nextRunTwo, startFireTimeDelay, "Until fire delay.");

        presenter.ack(null);

        Mockito.verifyNoMoreInteractions(cmdsPublisher);
        Mockito.doReturn(fireTime).when(selectIO).nowMilliUTC();

        final long nextRunThree = capturedRunnable.run();

        Assert.assertEquals(nextRunThree, -1, "After fire delay..");

        final ArgumentCaptor<StopAllForMarketNumberCmd> cmdCapture = ArgumentCaptor.forClass(StopAllForMarketNumberCmd.class);
        Mockito.verify(cmdsPublisher).publish(cmdCapture.capture());

        final StopAllForMarketNumberCmd cmd = cmdCapture.getValue();
        cmd.execute(cmdHandler);

        Mockito.verify(cmdHandler).stopAllForMarketNumber(Mockito.eq(true), Mockito.eq(reason));
        Mockito.verifyNoMoreInteractions(cmdsPublisher, cmdHandler);
    }

    @Test
    public void multiCallbackTest() {

        final String reason = "Test REASON.";
        final int numberTime = 70_000;
        final long startWarnDelay = numberTime - 60_000;
        final long warningTime = numberTime - startWarnDelay;
        final long startFireTimeDelay = numberTime - warningTime - 3_999;
        final long fireTime = numberTime - 1_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTime, reason, "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);

        presenter.setMarketNumber(marketNumberOne);

        final ArgumentCaptor<ISelectIORunnable> runnableArgumentCapture = ArgumentCaptor.forClass(ISelectIORunnable.class);
        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.anyLong(), runnableArgumentCapture.capture());
        final ISelectIORunnable capturedRunnable = runnableArgumentCapture.getValue();

        final long nextRunOne = capturedRunnable.run();

        Assert.assertEquals(nextRunOne, startWarnDelay, "Until warning delay.");

        Mockito.verifyNoMoreInteractions(cmdsPublisher);
        Mockito.doReturn(warningTime).when(selectIO).nowMilliUTC();

        final long nextRunTwo = capturedRunnable.run();

        Assert.assertEquals(nextRunTwo, startFireTimeDelay, "Until fire delay.");

        presenter.ack(null);

        Mockito.verifyNoMoreInteractions(cmdsPublisher);
        Mockito.doReturn(fireTime).when(selectIO).nowMilliUTC();

        final long nextRunThree = capturedRunnable.run();

        Assert.assertEquals(nextRunThree, -1, "After fire delay..");

        final ArgumentCaptor<StopAllForMarketNumberCmd> cmdCapture = ArgumentCaptor.forClass(StopAllForMarketNumberCmd.class);
        Mockito.verify(cmdsPublisher).publish(cmdCapture.capture());

        final StopAllForMarketNumberCmd cmd = cmdCapture.getValue();
        cmd.execute(cmdHandler);

        Mockito.verify(cmdHandler).stopAllForMarketNumber(Mockito.eq(true), Mockito.eq(reason));
        Mockito.verifyNoMoreInteractions(cmdsPublisher, cmdHandler);
    }

    @Test
    public void ackAtBadTimesTest() {

        final String reason = "Test REASON.";
        final int numberTime = 70_000;
        final long warningTime = numberTime - 10_000;
        final long fireTime = numberTime - 1_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTime, reason, "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);
        presenter.setMarketNumber(marketNumberOne);
        presenter.ack(null);

        final ArgumentCaptor<ISelectIORunnable> runnableArgumentCapture = ArgumentCaptor.forClass(ISelectIORunnable.class);
        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.anyLong(), runnableArgumentCapture.capture());
        final ISelectIORunnable capturedRunnable = runnableArgumentCapture.getValue();

        capturedRunnable.run();
        presenter.ack(null);

        Mockito.doReturn(warningTime).when(selectIO).nowMilliUTC();

        capturedRunnable.run();

        Mockito.doReturn(fireTime).when(selectIO).nowMilliUTC();

        capturedRunnable.run();
        presenter.ack(null);

        final ArgumentCaptor<StopAllForMarketNumberCmd> cmdCapture = ArgumentCaptor.forClass(StopAllForMarketNumberCmd.class);
        Mockito.verify(cmdsPublisher).publish(cmdCapture.capture());

        final StopAllForMarketNumberCmd cmd = cmdCapture.getValue();
        cmd.execute(cmdHandler);

        Mockito.verify(cmdHandler).stopAllForMarketNumber(Mockito.eq(false), Mockito.eq(reason));
        Mockito.verifyNoMoreInteractions(cmdsPublisher, cmdHandler);
    }

    @Test
    public void multiAckTest() {

        final String reason = "Test REASON.";
        final int numberTime = 70_000;
        final long warningTime = numberTime - 10_000;
        final long fireTime = numberTime - 1_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTime, reason, "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);
        presenter.setMarketNumber(marketNumberOne);

        final ArgumentCaptor<ISelectIORunnable> runnableArgumentCapture = ArgumentCaptor.forClass(ISelectIORunnable.class);
        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.anyLong(), runnableArgumentCapture.capture());
        final ISelectIORunnable capturedRunnable = runnableArgumentCapture.getValue();

        Mockito.doReturn(warningTime).when(selectIO).nowMilliUTC();

        capturedRunnable.run();
        presenter.ack(null);
        presenter.ack(null);
        presenter.ack(null);

        Mockito.doReturn(fireTime).when(selectIO).nowMilliUTC();

        capturedRunnable.run();

        final ArgumentCaptor<StopAllForMarketNumberCmd> cmdCapture = ArgumentCaptor.forClass(StopAllForMarketNumberCmd.class);
        Mockito.verify(cmdsPublisher).publish(cmdCapture.capture());

        final StopAllForMarketNumberCmd cmd = cmdCapture.getValue();
        cmd.execute(cmdHandler);

        Mockito.verify(cmdHandler).stopAllForMarketNumber(Mockito.eq(true), Mockito.eq(reason));
        Mockito.verifyNoMoreInteractions(cmdsPublisher, cmdHandler);
    }

    @Test
    public void onlySentOnceTest() {

        final String reason = "Test REASON.";
        final int numberTime = 70_000;
        final long warningTime = numberTime - 10_000;
        final long fireTime = numberTime - 1_000;

        final MarketNumbers marketNumberOne = new MarketNumbers(numberTime, reason, "today");

        final MarketNumberPresenter presenter = new MarketNumberPresenter(selectIO, uiLogger, cmdsPublisher);
        presenter.setMarketNumber(marketNumberOne);

        final ArgumentCaptor<ISelectIORunnable> runnableArgumentCapture = ArgumentCaptor.forClass(ISelectIORunnable.class);
        Mockito.verify(selectIO).addClockActionMilliSinceUTC(Mockito.anyLong(), runnableArgumentCapture.capture());
        final ISelectIORunnable capturedRunnable = runnableArgumentCapture.getValue();

        Mockito.doReturn(warningTime).when(selectIO).nowMilliUTC();

        capturedRunnable.run();

        Mockito.doReturn(fireTime).when(selectIO).nowMilliUTC();

        capturedRunnable.run();
        capturedRunnable.run();
        capturedRunnable.run();
        capturedRunnable.run();

        final ArgumentCaptor<StopAllForMarketNumberCmd> cmdCapture = ArgumentCaptor.forClass(StopAllForMarketNumberCmd.class);
        Mockito.verify(cmdsPublisher).publish(cmdCapture.capture());

        final StopAllForMarketNumberCmd cmd = cmdCapture.getValue();
        cmd.execute(cmdHandler);

        Mockito.verify(cmdHandler).stopAllForMarketNumber(Mockito.eq(false), Mockito.eq(reason));
        Mockito.verifyNoMoreInteractions(cmdsPublisher, cmdHandler);
    }
}
