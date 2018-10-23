package com.drwtrading.london.reddal.workingOrders;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.workingOrders.opxl.BestWorkingPriceForSymbol;
import org.testng.Assert;
import org.testng.annotations.Test;

public class WorkingOrdersByBestPriceTest {

    private static final String SYMBOL = "TEST AB";
    private static final String SOURCE = "SOURCE";

    @Test
    public static void basicTest() {

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, null, null, null, null);
    }

    @Test
    public static void addBidTest() {

        final BookSide side = BookSide.BID;
        final long bidPrice = 133 * Constants.NORMALISING_FACTOR;
        final long bidQty = 114;
        final long filledQty = 1;

        final WorkingOrder workingOrder =
                new WorkingOrder(1, 1, 1, SYMBOL, "TAG", side, AlgoType.HAWK, OrderType.MARKET, 1, bidPrice, bidQty, filledQty);
        final SourcedWorkingOrder sourcedWorkingOrder = new SourcedWorkingOrder(SOURCE, workingOrder);

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        final Long prevPrice = woByBest.setWorkingOrder(sourcedWorkingOrder);

        Assert.assertNull(prevPrice, "Previous price.");

        woByBest.getBestPrice(side);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, bidPrice, bidQty - filledQty, null, null);
    }

    @Test
    public static void removeBidTest() {

        final BookSide side = BookSide.BID;
        final long bidPrice = 133 * Constants.NORMALISING_FACTOR;
        final long bidQty = 114;
        final long filledQty = 1;

        final WorkingOrder workingOrder =
                new WorkingOrder(1, 1, 1, SYMBOL, "TAG", side, AlgoType.HAWK, OrderType.MARKET, 1, bidPrice, bidQty, filledQty);
        final SourcedWorkingOrder sourcedWorkingOrder = new SourcedWorkingOrder(SOURCE, workingOrder);

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        woByBest.setWorkingOrder(sourcedWorkingOrder);
        woByBest.removeWorkingOrder(sourcedWorkingOrder);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, null, null, null, null);
    }

    @Test
    public static void addAskTest() {

        final BookSide side = BookSide.ASK;
        final long askPrice = 163 * Constants.NORMALISING_FACTOR;
        final long askQty = 14;
        final long filledQty = 2;

        final WorkingOrder workingOrder =
                new WorkingOrder(1, 1, 1, SYMBOL, "TAG", side, AlgoType.HAWK, OrderType.MARKET, 1, askPrice, askQty, filledQty);
        final SourcedWorkingOrder sourcedWorkingOrder = new SourcedWorkingOrder(SOURCE, workingOrder);

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        woByBest.setWorkingOrder(sourcedWorkingOrder);
        woByBest.getBestPrice(side);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, null, null, askPrice, askQty - filledQty);
    }

    @Test
    public static void removeAskTest() {

        final BookSide side = BookSide.ASK;
        final long askPrice = 163 * Constants.NORMALISING_FACTOR;
        final long askQty = 14;
        final long filledQty = 5;

        final WorkingOrder workingOrder =
                new WorkingOrder(1, 1, 1, SYMBOL, "TAG", side, AlgoType.HAWK, OrderType.MARKET, 1, askPrice, askQty, filledQty);
        final SourcedWorkingOrder sourcedWorkingOrder = new SourcedWorkingOrder(SOURCE, workingOrder);

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        woByBest.setWorkingOrder(sourcedWorkingOrder);
        woByBest.removeWorkingOrder(sourcedWorkingOrder);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, null, null, null, null);
    }

    @Test
    public static void connectionLostTest() {

        final BookSide side = BookSide.BID;

        final WorkingOrder workingOrder = new WorkingOrder(1, 1, 1, SYMBOL, "TAG", side, AlgoType.HAWK, OrderType.MARKET, 1, 1, 1, 1);
        final SourcedWorkingOrder sourcedWorkingOrder = new SourcedWorkingOrder(SOURCE, workingOrder);

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        woByBest.setWorkingOrder(sourcedWorkingOrder);
        woByBest.getBestPrice(side);

        woByBest.connectionLost(SOURCE);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, null, null, null, null);
    }

    private static void assertBestPrice(final BestWorkingPriceForSymbol bestPrices, final String symbol, final Long bidPrice,
            final Long bidQty, final Long askPrice, final Long askQty) {

        Assert.assertEquals(bestPrices.symbol, symbol, "Symbol.");

        Assert.assertEquals(bestPrices.bidPrice, bidPrice, "Bid Price.");
        Assert.assertEquals(bestPrices.bidQty, bidQty, "Bid Qty.");

        Assert.assertEquals(bestPrices.askPrice, askPrice, "Ask Price.");
        Assert.assertEquals(bestPrices.askQty, askQty, "Ask Qty.");
    }
}
