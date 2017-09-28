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
    public void highlightingTwoOrders() {
        final IBookOrder firstOrder = Mockito.mock(IBookOrder.class);
        final IBookOrder secondOrder = Mockito.mock(IBookOrder.class);

        Mockito.when(firstOrder.getSide()).thenReturn(BookSide.BID);
        Mockito.when(firstOrder.getPrice()).thenReturn(1L);
        Mockito.when(firstOrder.getRemainingQty()).thenReturn(1337L);

        Mockito.when(secondOrder.getSide()).thenReturn(BookSide.BID);
        Mockito.when(secondOrder.getPrice()).thenReturn(1L);
        Mockito.when(secondOrder.getRemainingQty()).thenReturn(58008L);

        final WorkingOrderUpdate firstWorkingOrder = Mockito.mock(WorkingOrderUpdate.class);
        final WorkingOrderUpdate secondWorkingOrder = Mockito.mock(WorkingOrderUpdate.class);

        Mockito.when(firstWorkingOrder.getSide()).thenReturn(Side.BID);
        Mockito.when(firstWorkingOrder.getTotalQuantity()).thenReturn(1337);
        Mockito.when(firstWorkingOrder.getFilledQuantity()).thenReturn(0);
        Mockito.when(firstWorkingOrder.getWorkingOrderType()).thenReturn(WorkingOrderType.MARKET);

        Mockito.when(secondWorkingOrder.getSide()).thenReturn(Side.BID);
        Mockito.when(secondWorkingOrder.getTotalQuantity()).thenReturn(58008);
        Mockito.when(secondWorkingOrder.getFilledQuantity()).thenReturn(0);
        Mockito.when(secondWorkingOrder.getWorkingOrderType()).thenReturn(WorkingOrderType.MARKET);

        final WorkingOrderUpdateFromServer firstWorkingOrderContainer = new WorkingOrderUpdateFromServer("iddqd", firstWorkingOrder);
        final WorkingOrderUpdateFromServer secondWorkingOrderContainer = new WorkingOrderUpdateFromServer("UpUpDownDownLeftRightBA", secondWorkingOrder);


        final WorkingOrdersForSymbol workingOrdersForSymbol = new WorkingOrdersForSymbol("operation cwal");
        workingOrdersForSymbol.ordersByPrice.put(firstOrder.getPrice(), firstWorkingOrderContainer);
        workingOrdersForSymbol.ordersByPrice.put(secondOrder.getPrice(), secondWorkingOrderContainer);

        final ShredderBookView shredderBookView = new ShredderBookView(null, null, null,
                null, 10, workingOrdersForSymbol, null);


        ShreddedOrder shreddedOrder1 = new ShreddedOrder(0, 0, 0L, null, 0);
        ShreddedOrder shreddedOrder2 = new ShreddedOrder(0, 0, 0L, null, 0);
        shredderBookView.augmentIfOurOrder(firstOrder, shreddedOrder1);
        shredderBookView.augmentIfOurOrder(firstOrder, shreddedOrder2);
        Assert.assertTrue(shreddedOrder1.isOurs == true, "First order is not correctly identified");
        Assert.assertTrue(shreddedOrder2.isOurs == true, "Second order is not correctly identified");
    }
}
