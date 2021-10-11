package com.drwtrading.london.reddal.nibbler.tradingData;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.reddal.autopull.autopuller.onMD.AutoPuller;
import com.drwtrading.london.reddal.ladders.LadderPresenter;
import com.drwtrading.london.reddal.ladders.orders.OrdersPresenter;
import com.drwtrading.london.reddal.ladders.shredders.ShredderPresenter;
import com.drwtrading.london.reddal.nibblers.tradingData.LadderInfoListener;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCSupportedSymbol;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import org.jetlang.channels.Channel;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class LadderInfoListenerTest {

    private final LadderPresenter ladderPresenter = Mockito.mock(LadderPresenter.class);
    private final OrdersPresenter orderPresenter = Mockito.mock(OrdersPresenter.class);
    private final ShredderPresenter shredderPresenter = Mockito.mock(ShredderPresenter.class);
    private final AutoPuller autoPuller = Mockito.mock(AutoPuller.class);

    private final WorkingOrder workingOrderOne = Mockito.mock(WorkingOrder.class);
    private final WorkingOrder workingOrderTwo = Mockito.mock(WorkingOrder.class);

    @Mock
    private Channel<GTCSupportedSymbol> supportedGTCSymbols;

    @Captor
    private ArgumentCaptor<SourcedWorkingOrder> workingOrderCapture;

    @BeforeMethod
    public void setup() {

        Mockito.reset(ladderPresenter, orderPresenter, shredderPresenter, autoPuller);
        MockitoAnnotations.initMocks(this);

        Mockito.doReturn(AlgoType.QUOTE).when(workingOrderOne).getAlgoType();
        Mockito.doReturn(AlgoType.QUOTE).when(workingOrderTwo).getAlgoType();
    }

    @Test
    public void addWorkingOrderTest() {

        final String source = "TEST";
        final LadderInfoListener listener =
                new LadderInfoListener(source, ladderPresenter, orderPresenter, shredderPresenter, autoPuller, supportedGTCSymbols);

        listener.addWorkingOrder(workingOrderOne);

        Mockito.verify(ladderPresenter).setWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(orderPresenter).setWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(shredderPresenter).setWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(autoPuller).setWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);
    }

    @Test
    public void updateWorkingOrderTest() {

        final String source = "NIBBLER";
        final LadderInfoListener listener =
                new LadderInfoListener(source, ladderPresenter, orderPresenter, shredderPresenter, autoPuller, supportedGTCSymbols);

        listener.addWorkingOrder(workingOrderOne);
        listener.updateWorkingOrder(workingOrderOne);

        Mockito.verify(ladderPresenter, Mockito.times(2)).setWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(orderPresenter, Mockito.times(2)).setWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(shredderPresenter, Mockito.times(2)).setWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(autoPuller, Mockito.times(2)).setWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);
    }

    @Test
    public void deleteWorkingOrderTest() {

        final String source = "SOMEWHERE";
        final LadderInfoListener listener =
                new LadderInfoListener(source, ladderPresenter, orderPresenter, shredderPresenter, autoPuller, supportedGTCSymbols);

        listener.addWorkingOrder(workingOrderOne);
        listener.deleteWorkingOrder(workingOrderOne);

        Mockito.verify(ladderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(orderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(shredderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(autoPuller).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);
    }

    @Test
    public void disconnectWorkingOrderTest() {

        final String source = "SOMEWHERE";
        final LadderInfoListener listener =
                new LadderInfoListener(source, ladderPresenter, orderPresenter, shredderPresenter, autoPuller, supportedGTCSymbols);

        listener.addWorkingOrder(workingOrderOne);
        listener.connectionLost(source);

        Mockito.verify(ladderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(orderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(shredderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(autoPuller).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);
    }

    @Test
    public void deleteThenDisconnectedTest() {

        final String source = "SOMEWHERE";
        final LadderInfoListener listener =
                new LadderInfoListener(source, ladderPresenter, orderPresenter, shredderPresenter, autoPuller, supportedGTCSymbols);

        listener.addWorkingOrder(workingOrderOne);
        listener.deleteWorkingOrder(workingOrderOne);
        listener.connectionLost(source);

        Mockito.verify(ladderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(orderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(shredderPresenter).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);

        Mockito.verify(autoPuller).deleteWorkingOrder(workingOrderCapture.capture());
        checkCapturedObject(source, workingOrderOne);
    }

    private void checkCapturedObject(final String nibblerName, final WorkingOrder workingOrder) {

        final SourcedWorkingOrder sourcedWorkingOrder = workingOrderCapture.getValue();
        Assert.assertEquals(sourcedWorkingOrder.source, nibblerName, "Source.");
        Assert.assertEquals(sourcedWorkingOrder.order, workingOrder, "Order.");
    }
}
