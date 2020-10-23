package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.websockets.WebSocketClient;
import com.drwtrading.websockets.WebSocketInboundData;
import org.mockito.Mockito;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.EnumSet;

public class QuotingObligationsPresenterTest {

    public static final User USER = User.CMILLER;
    public static final String NIBBLER = "TEST_NIBBLER";
    public static final String SYMBOL = "VOD LN";

    private final SelectIO selectIO = Mockito.mock(SelectIO.class);
    private final UILogger webLog = Mockito.mock(UILogger.class);
    private final NibblerClientHandler nibblerHandler = Mockito.mock(NibblerClientHandler.class);
    private final WebSocketInboundData inboundData = Mockito.mock(WebSocketInboundData.class);
    private final WebSocketClient webClient = Mockito.mock(WebSocketClient.class);

    @BeforeMethod
    public void reset() {
        Mockito.reset(selectIO, webLog, nibblerHandler, inboundData, webClient);
        Mockito.doReturn(webClient).when(inboundData).getClient();
        Mockito.doReturn(USER.username).when(webClient).getUserName();
    }

    @Test
    public void doNotStartQuotingDisabledTest() {

        final QuotingObligationsPresenter presenter =
                new QuotingObligationsPresenter(EnumSet.of(StackCommunity.DM, StackCommunity.FI), selectIO, webLog);

        final QuotingState quotingState = new QuotingState(1, 1, SYMBOL, false, SYMBOL);
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, quotingState);

        presenter.setEnabledState("DM", SYMBOL, null);
        presenter.startStrategy(StackCommunity.DM, SYMBOL, USER);
        presenter.everythingOn("DM", inboundData);

        Mockito.verify(nibblerHandler, Mockito.never()).startQuoter(1, USER);

        presenter.setEnabledState("DM", SYMBOL, null);
        presenter.startStrategy(StackCommunity.DM, SYMBOL, USER);

        Mockito.verify(nibblerHandler).startQuoter(1, USER);
    }

    @Test
    public void fiDMSplitTest() {

        final QuotingObligationsPresenter presenter =
                new QuotingObligationsPresenter(EnumSet.of(StackCommunity.DM, StackCommunity.FI), selectIO, webLog);

        final QuotingState quotingState = new QuotingState(1, 1, SYMBOL, false, SYMBOL);
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, quotingState);

        presenter.setEnabledState("DM", SYMBOL, null);
        presenter.startStrategy(StackCommunity.DM, SYMBOL, USER);
        presenter.everythingOn("DM", inboundData);

        Mockito.verify(nibblerHandler, Mockito.never()).startQuoter(1, USER);

        presenter.setEnabledState("DM", SYMBOL, null);
        presenter.startStrategy(StackCommunity.DM, SYMBOL, USER);

        Mockito.verify(nibblerHandler).startQuoter(1, USER);
        Mockito.reset(nibblerHandler);

        presenter.setSymbol(StackCommunity.FI, SYMBOL);
        presenter.stopStrategy("DM", SYMBOL, null);
        Mockito.verifyZeroInteractions(nibblerHandler);
        presenter.stopStrategy("FI", SYMBOL, null);
        Mockito.verify(nibblerHandler).stopQuoter(1);
        Mockito.verify(nibblerHandler).batchComplete();
    }

    @Test
    public void disableQuotingTest() {
        final QuotingObligationsPresenter presenter =
                new QuotingObligationsPresenter(EnumSet.of(StackCommunity.DM, StackCommunity.FI), selectIO, webLog);

        final QuotingState quotingState = new QuotingState(1, 1, SYMBOL, false, SYMBOL);
        presenter.setNibblerHandler(NIBBLER, nibblerHandler);
        presenter.setQuotingState(NIBBLER, quotingState);

        presenter.setQuotingState(NIBBLER, new QuotingState(1, 1, SYMBOL, true, SYMBOL));
        presenter.setEnabledState("DM", SYMBOL, null);

        Mockito.verify(nibblerHandler).stopQuoter(1);
    }

}