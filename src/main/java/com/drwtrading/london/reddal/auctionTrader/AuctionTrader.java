package com.drwtrading.london.reddal.auctionTrader;

import com.drwtrading.london.eeif.nibbler.transport.data.types.AlgoType;
import com.drwtrading.london.eeif.nibbler.transport.data.types.OrderType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.marketData.book.ticks.ITickTable;
import com.drwtrading.london.eeif.utils.staticData.IndexConstant;
import com.drwtrading.london.indy.transport.data.IndexConstituent;
import com.drwtrading.london.indy.transport.data.IndexDef;
import com.drwtrading.london.reddal.data.TradeTracker;
import com.drwtrading.london.reddal.data.ibook.DepthBookSubscriber;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.IOrderCmd;
import com.drwtrading.london.reddal.orderManagement.remoteOrder.cmds.SubmitOrderCmd;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;

public class AuctionTrader {

    private static final IndexConstant OMX_INDEX = IndexConstant.OMX;

    private final Publisher<IOrderCmd> submitChannel;

    private final DepthBookSubscriber bookSubscriber;
    private final Map<String, MDForSymbol> books;

    private IndexDef index;

    public AuctionTrader(final DepthBookSubscriber bookSubscriber, final Publisher<IOrderCmd> submitChannel) {

        this.bookSubscriber = bookSubscriber;
        this.submitChannel = submitChannel;

        this.books = new HashMap<>();
    }

    public void setIndex(final IndexDef index) {

        if (OMX_INDEX.name().equals(index.name)) {

            System.out.println("INDEX FOUND: " + index);

            this.index = index;

            for (final IndexConstituent constituent : index.constituents) {

                final String symbol = constituent.instDef.bbgCode;
                final MDForSymbol mdForSymbol = bookSubscriber.subscribeForMD(symbol, this);
                books.put(symbol, mdForSymbol);
            }
        }
    }

    public void submit(final SubmitAuctionBasket submit) {

        if (null != index) {

            for (final IndexConstituent constituent : index.constituents) {

                final int numShares = (int) (submit.numOfBaskets * constituent.noOfShares);
                final MDForSymbol book = books.get(constituent.instDef.bbgCode);

                if (0 < numShares && null != book) {

                    final TradeTracker tradeTracker = book.getTradeTracker();

                    final long refPrice = tradeTracker.getLastPrice();

                    if (0 < refPrice) {

                        final double bpsWiden = submit.bps / 1_00_00d;

                        final ITickTable tickTable = book.getBook().getTickTable();
                        final long bidPrice = tickTable.roundAwayToTick(BookSide.BID, (long) (refPrice * (1 - bpsWiden)));
                        final long askPrice = tickTable.roundAwayToTick(BookSide.ASK, (long) (refPrice * (1 + bpsWiden)));

                        final SubmitOrderCmd bidSubmit =
                                new SubmitOrderCmd(constituent.instDef.bbgCode, Constants::NO_OP, submit.trader, BookSide.BID,
                                        OrderType.LIMIT, AlgoType.MANUAL, "AuctQuoter", bidPrice, numShares);
                        final SubmitOrderCmd askSubmit =
                                new SubmitOrderCmd(constituent.instDef.bbgCode, Constants::NO_OP, submit.trader, BookSide.ASK,
                                        OrderType.LIMIT, AlgoType.MANUAL, "AuctQuoter", askPrice, numShares);

                        System.out.println("BID: " + bidSubmit);
                        System.out.println("ASK: " + askSubmit);

                        //                        submitChannel.publish(bidSubmit);
                        //                        submitChannel.publish(askSubmit);
                    }
                }
            }
        }
    }
}
