package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import eeif.execution.Side;
import eeif.execution.WorkingOrderType;
import eeif.execution.WorkingOrderUpdate;
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
    }

    @Test
    public void highlightingTwoOrdersTest() {

        final String symbol = "operation cwal";
        final long price = 1;

        final WorkingOrderUpdate firstWorkingOrder = Mockito.mock(WorkingOrderUpdate.class);
        final WorkingOrderUpdate secondWorkingOrder = Mockito.mock(WorkingOrderUpdate.class);

        Mockito.doReturn(symbol).when(firstWorkingOrder).getSymbol();
        Mockito.doReturn(price).when(firstWorkingOrder).getPrice();
        Mockito.when(firstWorkingOrder.getSide()).thenReturn(Side.BID);
        Mockito.when(firstWorkingOrder.getTotalQuantity()).thenReturn(1337);
        Mockito.when(firstWorkingOrder.getFilledQuantity()).thenReturn(0);
        Mockito.when(firstWorkingOrder.getWorkingOrderType()).thenReturn(WorkingOrderType.MARKET);

        Mockito.doReturn(symbol).when(secondWorkingOrder).getSymbol();
        Mockito.doReturn(price).when(secondWorkingOrder).getPrice();
        Mockito.when(secondWorkingOrder.getSide()).thenReturn(Side.BID);
        Mockito.when(secondWorkingOrder.getTotalQuantity()).thenReturn(58008);
        Mockito.when(secondWorkingOrder.getFilledQuantity()).thenReturn(0);
        Mockito.when(secondWorkingOrder.getWorkingOrderType()).thenReturn(WorkingOrderType.MARKET);

        final WorkingOrders workingOrdersForSymbol = new WorkingOrders();
        workingOrdersForSymbol.setWorkingOrder(workingOrderOne);
        workingOrdersForSymbol.setWorkingOrder(workingOrderTwo);

        final ShredderBookView shredderBookView = new ShredderBookView(null, null, null, null, 10, workingOrdersForSymbol, null);

        final ShreddedOrder shreddedOrder1 = new ShreddedOrder(0, 0, 0L, null, 0);
        final ShreddedOrder shreddedOrder2 = new ShreddedOrder(0, 0, 0L, null, 0);

        shredderBookView.augmentIfOurOrder(orderOne, shreddedOrder1);
        shredderBookView.augmentIfOurOrder(orderOne, shreddedOrder2);
        Assert.assertTrue(shreddedOrder1.isOurs, "First order is not correctly identified");
        Assert.assertTrue(shreddedOrder2.isOurs, "Second order is not correctly identified");
    }
}
