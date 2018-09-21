package com.drwtrading.london.reddal.shredders;

import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBookOrder;
import com.drwtrading.london.reddal.data.WorkingOrdersForSymbol;
import com.drwtrading.london.reddal.workingOrders.WorkingOrderUpdateFromServer;
import eeif.execution.Side;
import eeif.execution.WorkingOrderType;
import eeif.execution.WorkingOrderUpdate;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

public class ShredderBookViewTest {

    @Test
    public void highlightingTwoOrdersTest() {

        final String symbol = "operation cwal";
        final long price = 1;

        final IBookOrder firstOrder = Mockito.mock(IBookOrder.class);
        final IBookOrder secondOrder = Mockito.mock(IBookOrder.class);

        Mockito.when(firstOrder.getSide()).thenReturn(BookSide.BID);
        Mockito.when(firstOrder.getPrice()).thenReturn(price);
        Mockito.when(firstOrder.getRemainingQty()).thenReturn(1337L);

        Mockito.when(secondOrder.getSide()).thenReturn(BookSide.BID);
        Mockito.when(secondOrder.getPrice()).thenReturn(1L);
        Mockito.when(secondOrder.getRemainingQty()).thenReturn(58008L);

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

        final WorkingOrderUpdateFromServer firstWorkingOrderContainer = new WorkingOrderUpdateFromServer("iddqd", firstWorkingOrder);
        final WorkingOrderUpdateFromServer secondWorkingOrderContainer =
                new WorkingOrderUpdateFromServer("UpUpDownDownLeftRightBA", secondWorkingOrder);

        final WorkingOrdersForSymbol workingOrdersForSymbol = new WorkingOrdersForSymbol(symbol);
        workingOrdersForSymbol.onWorkingOrderUpdate(firstWorkingOrderContainer);
        workingOrdersForSymbol.onWorkingOrderUpdate(secondWorkingOrderContainer);

        final ShredderBookView shredderBookView = new ShredderBookView(null, null, null, null, 10, workingOrdersForSymbol, null);

        final ShreddedOrder shreddedOrder1 = new ShreddedOrder(0, 0, 0L, null, 0);
        final ShreddedOrder shreddedOrder2 = new ShreddedOrder(0, 0, 0L, null, 0);

        shredderBookView.augmentIfOurOrder(firstOrder, shreddedOrder1);
        shredderBookView.augmentIfOurOrder(firstOrder, shreddedOrder2);
        Assert.assertTrue(shreddedOrder1.isOurs, "First order is not correctly identified");
        Assert.assertTrue(shreddedOrder2.isOurs, "Second order is not correctly identified");
    }
}
