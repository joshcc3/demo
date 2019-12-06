package com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.WorkingOrder;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.BookMarketState;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.IBook;
import com.drwtrading.london.eeif.utils.marketData.book.IBookLevel;
import com.drwtrading.london.reddal.data.ibook.IMDSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCBettermentPrices;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.bulkOrderEntry.msgs.GTCBettermentPricesRequest;
import org.jetlang.channels.Publisher;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class BulkOrderMarketAnalyser {

    private final MDSource mdSource;
    private final IMDSubscriber mdSubscriber;

    private final Publisher<GTCBettermentPrices> gtcBettermentResponses;

    private final Map<String, MDForSymbol> mdForSymbols;

    public BulkOrderMarketAnalyser(final MDSource mdSource, final IMDSubscriber mdSubscriber,
            final Publisher<GTCBettermentPrices> gtcBettermentResponses) {

        this.mdSource = mdSource;
        this.mdSubscriber = mdSubscriber;

        this.gtcBettermentResponses = gtcBettermentResponses;

        this.mdForSymbols = new HashMap<>();
    }

    public void checkForBettermentPrices(final GTCBettermentPricesRequest request) {

        final EnumMap<BookSide, Map<String, Long>> bettermentPrices = getBettermentPrices(request.existingOrders);

        final GTCBettermentPrices response =
                new GTCBettermentPrices(request.requestTimeMilliSinceMidnight, request.responseChannel, bettermentPrices);
        gtcBettermentResponses.publish(response);
    }

    private EnumMap<BookSide, Map<String, Long>> getBettermentPrices(final Map<BookSide, Map<String, WorkingOrder>> workingOrders) {

        final Map<String, WorkingOrder> bidOrders = workingOrders.get(BookSide.BID);
        final Map<String, WorkingOrder> askOrders = workingOrders.get(BookSide.ASK);

        final Map<String, Long> bidPrices = new HashMap<>();
        final Map<String, Long> askPrices = new HashMap<>();

        addBidBettermentPrices(bidOrders, bidPrices);
        addAskBettermentPrices(askOrders, askPrices);

        final EnumMap<BookSide, Map<String, Long>> orderPrices = new EnumMap<>(BookSide.class);

        orderPrices.put(BookSide.BID, bidPrices);
        orderPrices.put(BookSide.ASK, askPrices);

        return orderPrices;
    }

    private void addBidBettermentPrices(final Map<String, WorkingOrder> orders, final Map<String, Long> bidPrices) {

        for (final WorkingOrder workingOrder : orders.values()) {

            final String symbol = workingOrder.getSymbol();
            final MDForSymbol md = getMDForSymbol(symbol);

            if (null != md.getBook()) {

                final IBook<?> book = md.getBook();

                final IBookLevel bestBid = book.getBestBid();
                final IBookLevel bestAsk = book.getBestAsk();

                if (BookMarketState.CONTINUOUS == book.getStatus() && null != bestBid && null != bestAsk) {

                    final long currentPrice = workingOrder.getPrice();

                    final long bidPrice = bestBid.getPrice();

                    if (currentPrice < bidPrice) {

                        final long bettermentPrice = book.getTickTable().getTicksIn(BookSide.BID, bidPrice, 1);

                        if (bettermentPrice < bestAsk.getPrice()) {

                            bidPrices.put(symbol, bettermentPrice);
                        }
                    }
                }
            }
        }
    }

    private void addAskBettermentPrices(final Map<String, WorkingOrder> orders, final Map<String, Long> askPrices) {

        for (final WorkingOrder workingOrder : orders.values()) {

            final String symbol = workingOrder.getSymbol();
            final MDForSymbol md = getMDForSymbol(symbol);

            if (null != md.getBook()) {

                final IBook<?> book = md.getBook();

                final IBookLevel bestBid = book.getBestBid();
                final IBookLevel bestAsk = book.getBestAsk();

                if (BookMarketState.CONTINUOUS == book.getStatus() && null != bestBid && null != bestAsk) {

                    final long currentPrice = workingOrder.getPrice();

                    final long askPrice = bestAsk.getPrice();

                    if (askPrice < currentPrice) {

                        final long bettermentPrice = book.getTickTable().getTicksIn(BookSide.ASK, askPrice, 1);

                        if (bestBid.getPrice() < bettermentPrice) {

                            askPrices.put(symbol, bettermentPrice);
                        }
                    }
                }
            }
        }
    }

    private MDForSymbol getMDForSymbol(final String symbol) {

        final MDForSymbol oldMDForSystem = mdForSymbols.get(symbol);
        if (null == oldMDForSystem) {
            final MDForSymbol mdForSymbol = mdSubscriber.subscribeForMD(symbol, this);
            mdForSymbols.put(symbol, mdForSymbol);
            return mdForSymbol;
        } else {
            return oldMDForSystem;
        }
    }
}
