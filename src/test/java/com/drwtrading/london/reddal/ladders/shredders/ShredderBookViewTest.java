package com.drwtrading.london.reddal.ladders.shredders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.reddal.workingOrders.SourcedWorkingOrder;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByID;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ShredderBookViewTest {

    private static final int WORKING_ORDER_ID_ONE = 1;
    private static final int WORKING_ORDER_ID_TWO = 2;

    private static final long ORDER_BOOK_ID_ONE = 12340;
    private static final long ORDER_BOOK_ID_TWO = 999123;

    private final IBookOrder orderOne = Mockito.mock(IBookOrder.class);
    private final IBookOrder orderTwo = Mockito.mock(IBookOrder.class);

    private final WorkingOrder workingOrderOne = Mockito.mock(WorkingOrder.class);
    private final WorkingOrder workingOrderTwo = Mockito.mock(WorkingOrder.class);

    @BeforeMethod
    public void setup() {

        Mockito.reset(orderOne, orderTwo, workingOrderOne, workingOrderTwo);

        Mockito.doReturn(ORDER_BOOK_ID_ONE).when(orderOne).getOrderID();
        Mockito.doReturn(ORDER_BOOK_ID_TWO).when(orderTwo).getOrderID();

        Mockito.doReturn(WORKING_ORDER_ID_ONE).when(workingOrderOne).getWorkingOrderID();
        Mockito.doReturn(WORKING_ORDER_ID_TWO).when(workingOrderTwo).getWorkingOrderID();

        Mockito.doReturn(ORDER_BOOK_ID_ONE).when(workingOrderOne).getBookOrderID();
        Mockito.doReturn(ORDER_BOOK_ID_TWO).when(workingOrderTwo).getBookOrderID();

        Mockito.doReturn(OrderType.LIMIT).when(workingOrderOne).getOrderType();
        Mockito.doReturn(OrderType.LIMIT).when(workingOrderTwo).getOrderType();

        Mockito.doReturn(AlgoType.MANUAL).when(workingOrderOne).getAlgoType();
        Mockito.doReturn(AlgoType.MANUAL).when(workingOrderTwo).getAlgoType();
    }

    @Test
    public void highlightingTwoOrdersTest() {

        final SourcedWorkingOrder sourcedOrderOne = new SourcedWorkingOrder("Nibbler 1", workingOrderOne);
        final SourcedWorkingOrder sourcedOrderTwo = new SourcedWorkingOrder("Nibbler 2", workingOrderTwo);

        final WorkingOrdersByID workingOrdersForSymbol = new WorkingOrdersByID();
        workingOrdersForSymbol.setWorkingOrder(sourcedOrderOne);
        workingOrdersForSymbol.setWorkingOrder(sourcedOrderTwo);

        final ShredderBookView shredderBookView = new ShredderBookView(null, null, null, null, 10, workingOrdersForSymbol, null);

        final ShreddedOrder shreddedOrder1 = new ShreddedOrder(0, 0, 0L, null, 0);
        final ShreddedOrder shreddedOrder2 = new ShreddedOrder(0, 0, 0L, null, 0);

        shredderBookView.augmentIfOurOrder(orderOne, shreddedOrder1);
        shredderBookView.augmentIfOurOrder(orderOne, shreddedOrder2);
        Assert.assertTrue(shreddedOrder1.isOurs, "First order is not correctly identified");
        Assert.assertTrue(shreddedOrder2.isOurs, "Second order is not correctly identified");
    }
}
