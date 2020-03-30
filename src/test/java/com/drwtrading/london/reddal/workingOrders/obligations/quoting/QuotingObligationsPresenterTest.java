package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.websockets.WebSocketInboundData;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class QuotingObligationsPresenterTest {

    public static final String NIBBLER = "TEST_NIBBLER";
    public static final String SYMBOL = "VOD LN";

    private final SelectIO selectIO = Mockito.mock(SelectIO.class);
    private final UILogger webLog = Mockito.mock(UILogger.class);
    private final NibblerClientHandler nibblerHandler = Mockito.mock(NibblerClientHandler.class);
    private final WebSocketInboundData inboundData = Mockito.mock(WebSocketInboundData.class);

    @BeforeMethod
    public void reset() {
        Mockito.reset(selectIO, webLog, nibblerHandler, inboundData);
    }

    @Test
    public void doNotStartQuotingDisabledTest() {
        final QuotingObligationsPresenter presenter = new QuotingObligationsPresenter(selectIO, webLog);

        final QuotingState quotingState = new QuotingState(1, 1, SYMBOL, false, SYMBOL);
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, quotingState);

        presenter.setEnabledState(SYMBOL);
        presenter.startStrategy(SYMBOL);
        presenter.everythingOn(inboundData);

        Mockito.verify(nibblerHandler, Mockito.never()).startQuoter(1);

        presenter.setEnabledState(SYMBOL);
        presenter.startStrategy(SYMBOL);

        Mockito.verify(nibblerHandler).startQuoter(1);
    }

    @Test
    public void disableQuotingTest() {
        final QuotingObligationsPresenter presenter = new QuotingObligationsPresenter(selectIO, webLog);

        final QuotingState quotingState = new QuotingState(1, 1, SYMBOL, false, SYMBOL);
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, quotingState);

        presenter.setQuotingState(NIBBLER, new QuotingState(1, 1, SYMBOL, true, SYMBOL));
        presenter.setEnabledState(SYMBOL);

        Mockito.verify(nibblerHandler).stopQuoter(1);
    }

}