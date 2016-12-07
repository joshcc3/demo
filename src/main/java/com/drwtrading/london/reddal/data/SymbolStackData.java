package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.stack.transport.data.stacks.Stack;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackOrderType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

import java.text.DecimalFormat;

public class SymbolStackData {

    private static final String SOURCE = "LADDER_UI";

    private static final DecimalFormat PRICE_DF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 1, 100);

    private static final int BID_PRICE_MULTIPLIER = -1;
    private static final int ASK_PRICE_MULTIPLIER = 1;

    private final String symbol;

    private StackClientHandler stackClient;

    private final LongMap<SymbolStackPriceLevel> bidStackLevels;
    private StackGroup bidStackGroup;
    private String bidFormattedPriceOffset;
    private long totalBidQty;

    private final LongMap<SymbolStackPriceLevel> askStackLevels;
    private StackGroup askStackGroup;
    private String askFormattedPriceOffset;
    private long totalAskQty;

    public SymbolStackData(final String symbol) {

        this.symbol = symbol;

        this.bidStackLevels = new LongMap<>();
        this.askStackLevels = new LongMap<>();

        this.bidFormattedPriceOffset = "---";
        this.askFormattedPriceOffset = "---";
    }

    public String getRemoteAppName() {
        return stackClient.getRemoteName();
    }

    public void setStackClientHandler(final StackClientHandler stackClient) {

        if (null != this.stackClient && !this.stackClient.equals(stackClient)) {
            throw new IllegalStateException("Two stack clients are providing stacks for the same instrument.");
        } else {
            this.stackClient = stackClient;
        }
    }

    public void stackConnectionLost() {

        bidStackLevels.clear();
        bidStackGroup = null;
        bidFormattedPriceOffset = "---";

        askStackLevels.clear();
        askStackGroup = null;
        askFormattedPriceOffset = "---";
    }

    public void setBidGroup(final StackGroup stackGroup) {

        bidStackGroup = stackGroup;
        bidFormattedPriceOffset = PRICE_DF.format(stackGroup.getPriceOffset() / (double) Constants.NORMALISING_FACTOR);
        totalBidQty = setGroup(bidStackLevels, stackGroup, BID_PRICE_MULTIPLIER);

    }

    public void setAskGroup(final StackGroup stackGroup) {

        askStackGroup = stackGroup;
        askFormattedPriceOffset = PRICE_DF.format(stackGroup.getPriceOffset() / (double) Constants.NORMALISING_FACTOR);
        totalAskQty = setGroup(askStackLevels, stackGroup, ASK_PRICE_MULTIPLIER);
    }

    private static long setGroup(final LongMap<SymbolStackPriceLevel> stackLevels, final StackGroup stackGroup,
            final long priceMultiplier) {

        stackLevels.clear();

        long result = 0;
        for (final StackType stackType : StackType.values()) {

            final Stack stack = stackGroup.getStack(stackType);
            StackLevel level = stack.getFirstLevel();

            while (null != level) {

                result += level.getRemainingQty();

                final long pullbackTicks = priceMultiplier * level.getPullbackTicks();
                final SymbolStackPriceLevel priceLevel = stackLevels.get(pullbackTicks);
                if (null == priceLevel) {

                    final SymbolStackPriceLevel newPriceLevel = new SymbolStackPriceLevel();
                    newPriceLevel.setStackLevel(stackType, level);
                    stackLevels.put(pullbackTicks, newPriceLevel);
                } else {

                    priceLevel.setStackLevel(stackType, level);
                }

                level = stack.next(level);
            }
        }

        return result;
    }

    public String getFormattedBidPriceOffset() {
        return bidFormattedPriceOffset;
    }

    public long getBidPriceOffsetTickSize() {
        return bidStackGroup.getPriceOffsetTickSize();
    }

    public boolean hasBestBid() {
        return !bidStackLevels.isEmpty();
    }

    public long getBestBid() {
        return getMostAggressiveTickOffset(bidStackGroup, BID_PRICE_MULTIPLIER);
    }

    public SymbolStackPriceLevel getBidPriceLevel(final long price) {
        return bidStackLevels.get(price);
    }

    public long getTotalBidQty() {
        return totalBidQty;
    }

    public String getFormattedAskPriceOffset() {
        return askFormattedPriceOffset;
    }

    public long getAskPriceOffsetTickSize() {
        return askStackGroup.getPriceOffsetTickSize();
    }

    public boolean hasBestAsk() {
        return !askStackLevels.isEmpty();
    }

    public long getBestAsk() {
        return getMostAggressiveTickOffset(askStackGroup, ASK_PRICE_MULTIPLIER);
    }

    public SymbolStackPriceLevel getAskPriceLevel(final long price) {
        return askStackLevels.get(price);
    }

    public long getTotalAskQty() {
        return totalAskQty;
    }

    private static long getMostAggressiveTickOffset(final StackGroup stackGroup, final long priceMultiplier) {

        long mostAggressiveTick = Long.MAX_VALUE;

        for (final StackType stackType : StackType.values()) {
            final Stack stack = stackGroup.getStack(stackType);
            final StackLevel level = stack.getFirstLevel();
            if (null != level) {
                mostAggressiveTick = Math.min(mostAggressiveTick, level.getPullbackTicks());
            }
        }

        return priceMultiplier * mostAggressiveTick;
    }

    public boolean improveBidStackPriceOffset(final long priceOffset) {

        if (null != bidStackGroup) {
            stackClient.updateStackPriceOffset(SOURCE, bidStackGroup.getStackID(), bidStackGroup.getPriceOffset() + priceOffset);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean improveAskStackPriceOffset(final long priceOffset) {

        if (null != askStackGroup) {
            stackClient.updateStackPriceOffset(SOURCE, askStackGroup.getStackID(), askStackGroup.getPriceOffset() + priceOffset);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean setBidStackEnabled(final StackType stackType, final boolean isEnabled) {

        if (null != bidStackGroup) {
            stackClient.setStackEnabled(SOURCE, bidStackGroup.getStackID(), stackType, isEnabled);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean setAskStackEnabled(final StackType stackType, final boolean isEnabled) {

        if (null != askStackGroup) {
            stackClient.setStackEnabled(SOURCE, askStackGroup.getStackID(), stackType, isEnabled);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean clearBidStack() {

        if (null != bidStackGroup) {
            stackClient.stackCleared(SOURCE, bidStackGroup.getStackID());
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean clearAskStack() {

        if (null != askStackGroup) {
            stackClient.stackCleared(SOURCE, askStackGroup.getStackID());
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean adjustBidStackLevels(final StackType stackType, final int tickAdjustment) {

        if (null != bidStackGroup) {
            stackClient.adjustStackLevels(SOURCE, bidStackGroup.getStackID(), stackType, tickAdjustment);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean adjustAskStackLevels(final StackType stackType, final int tickAdjustment) {

        if (null != askStackGroup) {
            stackClient.adjustStackLevels(SOURCE, askStackGroup.getStackID(), stackType, tickAdjustment);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean clearBidStackPrice(final int tickOffset) {

        if (null != bidStackGroup) {
            for (final StackType stackType : StackType.values()) {
                for (final StackOrderType orderType : StackOrderType.values()) {
                    stackClient.addStackQty(SOURCE, bidStackGroup.getStackID(), stackType, orderType, BID_PRICE_MULTIPLIER * tickOffset,
                            -Long.MAX_VALUE);
                }
            }
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean clearAskStackPrice(final int tickOffset) {

        if (null != askStackGroup) {
            for (final StackType stackType : StackType.values()) {
                for (final StackOrderType orderType : StackOrderType.values()) {
                    stackClient.addStackQty(SOURCE, askStackGroup.getStackID(), stackType, orderType, ASK_PRICE_MULTIPLIER * tickOffset,
                            -Long.MAX_VALUE);
                }
            }
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean setBidStackQty(final StackType stackType, final StackOrderType orderType, final int tickOffset, final long qty) {

        if (null != bidStackGroup) {
            stackClient.addStackQty(SOURCE, bidStackGroup.getStackID(), stackType, orderType, BID_PRICE_MULTIPLIER * tickOffset, qty);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean setAskStackQty(final StackType stackType, final StackOrderType orderType, final int tickOffset, final long qty) {

        if (null != askStackGroup) {
            stackClient.addStackQty(SOURCE, askStackGroup.getStackID(), stackType, orderType, ASK_PRICE_MULTIPLIER * tickOffset, qty);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean startBidStrategy() {
        if (null != bidStackGroup) {
            stackClient.startStrategy(bidStackGroup.getSymbol(), BookSide.BID);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean stopBidStrategy() {
        if (null != bidStackGroup) {
            stackClient.stopStrategy(bidStackGroup.getSymbol(), BookSide.BID);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean startAskStrategy() {
        if (null != askStackGroup) {
            stackClient.startStrategy(askStackGroup.getSymbol(), BookSide.ASK);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean stopAskStrategy() {
        if (null != askStackGroup) {
            stackClient.stopStrategy(askStackGroup.getSymbol(), BookSide.ASK);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }
}
