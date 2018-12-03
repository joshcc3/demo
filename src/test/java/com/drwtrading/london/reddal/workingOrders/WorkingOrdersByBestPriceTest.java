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

    private final String SYMBOL = "TEST AB";
    private final String SOURCE = "SOURCE";

    @Test
    public void basicTest() {

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, null, null, null, null);
    }

    @Test
    public void addBidTest() {

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
    public void removeBidTest() {

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
    public void addAskTest() {

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
    public void removeAskTest() {

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
    public void bothSidesPresentTest() {

        final long bidPrice = 10312300000L;
        final long askPrice = bidPrice + 201230000L;

        final SourcedWorkingOrder bidWorkingOrder = getWorkingOrder(1, BookSide.BID, bidPrice, 1);
        final SourcedWorkingOrder askWorkingOrder = getWorkingOrder(2, BookSide.ASK, askPrice, 1);

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        woByBest.setWorkingOrder(bidWorkingOrder);
        woByBest.setWorkingOrder(askWorkingOrder);

        final long bestBidFound = woByBest.getBestPrice(BookSide.BID);
        final long bestAskFound = woByBest.getBestPrice(BookSide.ASK);

        Assert.assertEquals(bestBidFound, bidPrice, "Best bid price.");
        Assert.assertEquals(bestAskFound, askPrice, "Best ask price.");
    }

    @Test
    public void connectionLostTest() {

        final long bidPrice = 10312300000L;
        final long askPrice = bidPrice + 201230000L;

        final SourcedWorkingOrder bidWorkingOrder = getWorkingOrder(1, BookSide.BID, bidPrice, 1);
        final SourcedWorkingOrder askWorkingOrder = getWorkingOrder(2, BookSide.ASK, askPrice, 1);

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        woByBest.setWorkingOrder(bidWorkingOrder);
        woByBest.setWorkingOrder(askWorkingOrder);

        woByBest.connectionLost(SOURCE);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, null, null, null, null);
    }

    @Test
    public void connectionReestablishedTest() {

        final int firstWOID = 1;
        final int secondWOID = firstWOID + 2;

        final long bidPrice = 10312300000L;
        final long askPrice = bidPrice + 201230000L;

        final long bidQty = 10L;
        final long askQty = 20L;

        final SourcedWorkingOrder bidWorkingOrderOne = getWorkingOrder(firstWOID, BookSide.BID, bidPrice, bidQty);
        final SourcedWorkingOrder askWorkingOrderOne = getWorkingOrder(secondWOID, BookSide.ASK, askPrice, askQty);

        final SourcedWorkingOrder bidWorkingOrderTwo = getWorkingOrder(secondWOID, BookSide.BID, bidPrice, bidQty);
        final SourcedWorkingOrder askWorkingOrderTwo = getWorkingOrder(firstWOID, BookSide.ASK, askPrice, askQty);

        final WorkingOrdersByBestPrice woByBest = new WorkingOrdersByBestPrice(SYMBOL);

        woByBest.setWorkingOrder(bidWorkingOrderOne);
        woByBest.setWorkingOrder(askWorkingOrderOne);

        woByBest.connectionLost(SOURCE);

        woByBest.setWorkingOrder(bidWorkingOrderTwo);
        woByBest.setWorkingOrder(askWorkingOrderTwo);

        final BestWorkingPriceForSymbol bestPrices = woByBest.getBestWorkingPrices();

        assertBestPrice(bestPrices, SYMBOL, bidPrice, bidWorkingOrderTwo.order.getOrderQty() - bidWorkingOrderTwo.order.getFilledQty(),
                askPrice, askWorkingOrderTwo.order.getOrderQty() - askWorkingOrderTwo.order.getFilledQty());
    }

    private SourcedWorkingOrder getWorkingOrder(final int woID, final BookSide side, final long price, final long qty) {

        final WorkingOrder workingOrder =
                new WorkingOrder(woID, 1, 1, SYMBOL, "TAG", side, AlgoType.HAWK, OrderType.MARKET, 1, price, qty, 1);
        return new SourcedWorkingOrder(SOURCE, workingOrder);
    }

    private void assertBestPrice(final BestWorkingPriceForSymbol bestPrices, final String symbol, final Long bidPrice, final Long bidQty,
            final Long askPrice, final Long askQty) {

        Assert.assertEquals(bestPrices.symbol, symbol, "Symbol.");

        Assert.assertEquals(bestPrices.bidPrice, bidPrice, "Bid Price.");
        Assert.assertEquals(bestPrices.bidQty, bidQty, "Bid Qty.");

        Assert.assertEquals(bestPrices.askPrice, askPrice, "Ask Price.");
        Assert.assertEquals(bestPrices.askQty, askQty, "Ask Qty.");
    }
}
