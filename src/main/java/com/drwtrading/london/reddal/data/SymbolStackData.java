package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.stack.transport.data.stacks.Stack;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackOrderType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

import java.text.DecimalFormat;

public class SymbolStackData {

    private static final String SOURCE = "LADDER_UI";

    private static final DecimalFormat PRICE_DF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 100);

    private static final int BID_PRICE_MULTIPLIER = -1;
    private static final int ASK_PRICE_MULTIPLIER = 1;

    private final String symbol;

    private StackClientHandler stackClient;

    private final LongMap<SymbolStackPriceLevel> bidStackLevels;
    private StackGroup bidStackGroup;
    private String bidFormattedPriceOffsetBPS;
    private long totalBidQty;
    private final boolean[] enabledBidStacks;

    private final LongMap<SymbolStackPriceLevel> askStackLevels;
    private StackGroup askStackGroup;
    private String askFormattedPriceOffsetBPS;
    private long totalAskQty;
    private final boolean[] enabledAskStacks;

    private double priceOffsetTickSize;
    private int stackGroupTickMultiplier;
    private double stackAlignmentTickToBPS;

    public SymbolStackData(final String symbol) {

        this.symbol = symbol;

        this.bidStackLevels = new LongMap<>();
        this.askStackLevels = new LongMap<>();

        this.bidFormattedPriceOffsetBPS = "---";
        this.askFormattedPriceOffsetBPS = "---";

        this.enabledBidStacks = new boolean[StackType.values().length];
        this.enabledAskStacks = new boolean[StackType.values().length];

        this.priceOffsetTickSize = 0d;
        this.stackGroupTickMultiplier = 1;
        this.stackAlignmentTickToBPS = 0d;
    }

    public void setStackClientHandler(final StackClientHandler stackClient) {

        if (null != this.stackClient && !this.stackClient.equals(stackClient)) {
            throw new IllegalStateException("Two stack clients are providing stacks for the same instrument.");
        } else {
            this.stackClient = stackClient;
        }
    }

    public void stackConnectionLost(final String remoteAppName) {

        if (null != this.stackClient && remoteAppName.equals(this.stackClient.getRemoteUser())) {

            bidStackLevels.clear();
            bidStackGroup = null;
            bidFormattedPriceOffsetBPS = "---";

            askStackLevels.clear();
            askStackGroup = null;
            askFormattedPriceOffsetBPS = "---";

            priceOffsetTickSize = 0d;
            stackGroupTickMultiplier = 1;
            stackAlignmentTickToBPS = 0d;

            for (final StackType stackType : StackType.values()) {
                enabledBidStacks[stackType.ordinal()] = false;
                enabledAskStacks[stackType.ordinal()] = false;
            }
        }
    }

    public void setBidGroup(final StackGroup stackGroup) {

        bidStackGroup = stackGroup;
        bidFormattedPriceOffsetBPS = PRICE_DF.format(stackGroup.getPriceOffsetBPS());
        totalBidQty = setGroup(bidStackLevels, stackGroup, BID_PRICE_MULTIPLIER);

        for (final StackType stackType : StackType.values()) {
            final Stack stack = stackGroup.getStack(stackType);
            enabledBidStacks[stackType.ordinal()] = stack.isEnabled();
        }

        priceOffsetTickSize = stackGroup.getPriceOffsetTickSize();
        stackGroupTickMultiplier = stackGroup.getTickMultiplier();
        stackAlignmentTickToBPS = stackGroup.getStackAlignmentTickToBPS();
    }

    public void setAskGroup(final StackGroup stackGroup) {

        askStackGroup = stackGroup;
        askFormattedPriceOffsetBPS = PRICE_DF.format(stackGroup.getPriceOffsetBPS());
        totalAskQty = setGroup(askStackLevels, stackGroup, ASK_PRICE_MULTIPLIER);

        for (final StackType stackType : StackType.values()) {
            final Stack stack = stackGroup.getStack(stackType);
            enabledAskStacks[stackType.ordinal()] = stack.isEnabled();
        }
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

    public boolean isBidStackEnabled(final StackType stackType) {
        return enabledBidStacks[stackType.ordinal()];
    }

    public String getFormattedBidPriceOffsetBPS() {
        return bidFormattedPriceOffsetBPS;
    }

    public String getFormattedBidPriceOffsetBPS(final double bpsOffset) {
        return PRICE_DF.format(bidStackGroup.getPriceOffsetBPS() + bpsOffset);
    }

    public double getPriceOffsetTickSize() {
        return priceOffsetTickSize;
    }

    public int getStackGroupTickMultiplier() {
        return stackGroupTickMultiplier;
    }

    public double getStackAlignmentTickToBPS() {
        return stackAlignmentTickToBPS;
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

    public boolean isAskStackEnabled(final StackType stackType) {
        return enabledAskStacks[stackType.ordinal()];
    }

    public String getFormattedAskPriceOffset() {
        return askFormattedPriceOffsetBPS;
    }

    public String getFormattedAskPriceOffsetBPS(final double bpsOffset) {
        return PRICE_DF.format(askStackGroup.getPriceOffsetBPS() + bpsOffset);
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

    public boolean setStackGroupUpdate(final double tickSize, final int tickMultiplier, final double stackAlignmentTickToBPS) {

        if (null != bidStackGroup) {
            stackClient.updateStackGroup(SOURCE, bidStackGroup.getStackID(), bidStackGroup.getPriceOffsetBPS(), tickSize, tickMultiplier,
                    stackAlignmentTickToBPS);
            stackClient.updateStackGroup(SOURCE, askStackGroup.getStackID(), askStackGroup.getPriceOffsetBPS(), tickSize, tickMultiplier,
                    stackAlignmentTickToBPS);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean improveBidStackPriceOffset(final double priceOffset) {

        if (null != bidStackGroup) {
            final double newOffset = bidStackGroup.getPriceOffsetBPS() + priceOffset;
            stackClient.updateStackGroup(SOURCE, bidStackGroup.getStackID(), newOffset, bidStackGroup.getPriceOffsetTickSize(),
                    bidStackGroup.getTickMultiplier(), bidStackGroup.getStackAlignmentTickToBPS());
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean improveAskStackPriceOffset(final double priceOffset) {

        if (null != askStackGroup) {
            final double newOffset = askStackGroup.getPriceOffsetBPS() + priceOffset;
            stackClient.updateStackGroup(SOURCE, askStackGroup.getStackID(), newOffset, askStackGroup.getPriceOffsetTickSize(),
                    askStackGroup.getTickMultiplier(), askStackGroup.getStackAlignmentTickToBPS());
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

    public boolean adjustBidStackLevels(final int tickAdjustment) {

        if (null != bidStackGroup) {
            for (final StackType stackType : StackType.values()) {
                stackClient.adjustStackLevels(SOURCE, bidStackGroup.getStackID(), stackType, tickAdjustment);
            }
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean adjustAskStackLevels(final int tickAdjustment) {

        if (null != askStackGroup) {

            for (final StackType stackType : StackType.values()) {
                stackClient.adjustStackLevels(SOURCE, askStackGroup.getStackID(), stackType, tickAdjustment);
            }
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean clearBidStackPrice(final int tickOffset) {

        if (null != bidStackGroup) {
            for (final StackType stackType : StackType.values()) {
                final SymbolStackPriceLevel stackPriceLevel = bidStackLevels.get(tickOffset);
                if (null != stackPriceLevel) {
                    final StackLevel level = stackPriceLevel.getStackType(stackType);
                    if (null != level) {
                        for (final StackOrderType orderType : StackOrderType.values()) {
                            if (0 < level.getOrderTypeQty(orderType)) {
                                stackClient.addStackQty(SOURCE, bidStackGroup.getStackID(), stackType, orderType,
                                        BID_PRICE_MULTIPLIER * tickOffset, -Long.MAX_VALUE);
                            }
                        }
                    }
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
                final SymbolStackPriceLevel stackPriceLevel = askStackLevels.get(tickOffset);
                if (null != stackPriceLevel) {
                    final StackLevel level = stackPriceLevel.getStackType(stackType);
                    if (null != level) {
                        for (final StackOrderType orderType : StackOrderType.values()) {
                            if (0 < level.getOrderTypeQty(orderType)) {
                                stackClient.addStackQty(SOURCE, askStackGroup.getStackID(), stackType, orderType,
                                        ASK_PRICE_MULTIPLIER * tickOffset, -Long.MAX_VALUE);
                            }
                        }
                    }
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
