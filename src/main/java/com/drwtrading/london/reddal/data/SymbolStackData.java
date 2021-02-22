package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.SpreadnoughtTheo;
import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.TheoValue;
import com.drwtrading.london.eeif.stack.transport.data.stacks.Stack;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackOrderType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.reddal.picard.IPicardSpotter;
import com.drwtrading.london.reddal.premium.IPremiumCalc;

import java.text.DecimalFormat;
import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;

public class SymbolStackData {

    private static final String SOURCE = "LADDER_UI";

    private static final DecimalFormat PRICE_DF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 100);

    private static final int BID_PRICE_MULTIPLIER = -1;
    private static final int ASK_PRICE_MULTIPLIER = 1;

    private final String symbol;

    private StackClientHandler stackClient;

    private final StacksLaserLineCalc stacksLaserLineCalc;

    private final LongMap<SymbolStackPriceLevel> bidStackLevels;
    private StackGroup bidStackGroup;
    private String bidFormattedPriceOffsetBPS;
    private long totalBidQty;
    private final boolean[] enabledBidStacks;
    private double bidMostAggressiveOffsetBPS;

    private final LongMap<SymbolStackPriceLevel> askStackLevels;
    private StackGroup askStackGroup;
    private String askFormattedPriceOffsetBPS;
    private long totalAskQty;
    private final boolean[] enabledAskStacks;
    private double askMostAggressiveOffsetBPS;

    private double priceOffsetTickSize;
    private double stackAlignmentTickToBPS;

    public SymbolStackData(final String symbol, final IPicardSpotter picardSpotter, final IPremiumCalc premiumCalc) {

        this.symbol = symbol;

        this.stacksLaserLineCalc = new StacksLaserLineCalc(symbol, picardSpotter, premiumCalc);

        this.bidStackLevels = new LongMap<>();
        this.askStackLevels = new LongMap<>();

        this.bidFormattedPriceOffsetBPS = "---";
        this.askFormattedPriceOffsetBPS = "---";

        this.enabledBidStacks = new boolean[StackType.values().length];
        this.enabledAskStacks = new boolean[StackType.values().length];

        this.priceOffsetTickSize = 0d;
        this.stackAlignmentTickToBPS = 0d;
    }

    public void setStackClientHandler(final StackClientHandler stackClient) {

        if (null != this.stackClient && !this.stackClient.equals(stackClient)) {
            throw new IllegalStateException(
                    "Two stack clients [" + this.stackClient.getRemoteUser() + "] and [" + stackClient.getRemoteUser() +
                            "] are providing stacks for the same instrument [" + symbol + "].");
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

            stacksLaserLineCalc.setBidStackGroup(null);
            stacksLaserLineCalc.setAskStackGroup(null);

            priceOffsetTickSize = 0d;
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

        stacksLaserLineCalc.setBidStackGroup(stackGroup);

        boolean isStackPresent = false;
        for (final StackType stackType : StackType.values()) {

            final Stack stack = stackGroup.getStack(stackType);
            enabledBidStacks[stackType.ordinal()] = stack.isEnabled();

            isStackPresent |= null != stack.getFirstLevel();
        }

        priceOffsetTickSize = stackGroup.getPriceOffsetTickSize();
        stackAlignmentTickToBPS = stackGroup.getStackAlignmentTickToBPS();

        if (isStackPresent) {
            bidMostAggressiveOffsetBPS = stackGroup.getPriceOffsetBPS() +
                    stackAlignmentTickToBPS * getMostAggressiveTickOffset(stackGroup, BID_PRICE_MULTIPLIER);
        } else {
            bidMostAggressiveOffsetBPS = Double.NaN;
        }
    }

    public void setAskGroup(final StackGroup stackGroup) {

        askStackGroup = stackGroup;
        askFormattedPriceOffsetBPS = PRICE_DF.format(stackGroup.getPriceOffsetBPS());
        totalAskQty = setGroup(askStackLevels, stackGroup, ASK_PRICE_MULTIPLIER);

        stacksLaserLineCalc.setAskStackGroup(stackGroup);

        boolean isStackPresent = false;
        for (final StackType stackType : StackType.values()) {

            final Stack stack = stackGroup.getStack(stackType);
            enabledAskStacks[stackType.ordinal()] = stack.isEnabled();

            isStackPresent |= null != stack.getFirstLevel();
        }

        final double stackAlignmentTickToBPS = stackGroup.getStackAlignmentTickToBPS();

        if (isStackPresent) {
            askMostAggressiveOffsetBPS = stackGroup.getPriceOffsetBPS() +
                    stackAlignmentTickToBPS * getMostAggressiveTickOffset(stackGroup, ASK_PRICE_MULTIPLIER);
        } else {
            askMostAggressiveOffsetBPS = Double.NaN;
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

    public void overrideStackData(final LaserLine laserLineValue) {
        this.stacksLaserLineCalc.overrideLaserLine(laserLineValue);
    }

    public TheoValue getTheoValue() {
        return stacksLaserLineCalc.getTheoValue();
    }

    public void setTheoValue(final TheoValue theoValue) {
        this.stacksLaserLineCalc.setTheoValue(theoValue);
    }

    public void setSpreadnoughtTheo(final SpreadnoughtTheo theo) {
        this.stacksLaserLineCalc.setSpreadnoughtTheo(theo);
    }

    public LaserLine getNavLaserLine() {
        return stacksLaserLineCalc.getNavLaserLine();
    }

    public LaserLine getTheoLaserLine() {
        return stacksLaserLineCalc.getTheoLaserLine();
    }

    public LaserLine getSpreadnoughtLaserLine() {
        return stacksLaserLineCalc.getSpreadnoughtLaserLine();
    }

    public Collection<LaserLine> getLaserLines() {
        return stacksLaserLineCalc.getLaserLines();
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

    public double getBidTopOrderOffsetBPS() {
        return bidMostAggressiveOffsetBPS;
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

    public double getAskTopOrderOffsetBPS() {
        return askMostAggressiveOffsetBPS;
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

    public void setStackGroupUpdate(final double tickSize, final double stackAlignmentTickToBPS) {

        if (null != bidStackGroup) {
            stackClient.updateStackGroup(SOURCE, bidStackGroup.getStackID(), bidStackGroup.getPriceOffsetBPS(), tickSize,
                    stackAlignmentTickToBPS);
            stackClient.updateStackGroup(SOURCE, askStackGroup.getStackID(), askStackGroup.getPriceOffsetBPS(), tickSize,
                    stackAlignmentTickToBPS);
            stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean improveBidStackPriceOffset(final double priceOffset) {

        return improveStackPriceOffset(bidStackGroup, priceOffset);
    }

    public boolean improveAskStackPriceOffset(final double priceOffset) {

        return improveStackPriceOffset(askStackGroup, priceOffset);
    }

    private boolean improveStackPriceOffset(final StackGroup stackGroup, final double priceOffset) {

        if (null != stackGroup) {
            final double newOffset = stackGroup.getPriceOffsetBPS() + priceOffset;
            stackClient.updateStackGroup(SOURCE, stackGroup.getStackID(), newOffset, stackGroup.getPriceOffsetTickSize(),
                    stackGroup.getStackAlignmentTickToBPS());
            return stackClient.batchComplete();
        } else {
            return false;
        }
    }

    public void setBidStackEnabled(final StackType stackType, final boolean isEnabled) {

        if (null != bidStackGroup) {
            stackClient.setStackEnabled(SOURCE, bidStackGroup.getStackID(), stackType, isEnabled);
            stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public void setAskStackEnabled(final StackType stackType, final boolean isEnabled) {

        if (null != askStackGroup) {
            stackClient.setStackEnabled(SOURCE, askStackGroup.getStackID(), stackType, isEnabled);
            stackClient.batchComplete();
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

    public void clearBidStack(final StackType stackType, final StackOrderType orderType) {

        clearStack(bidStackGroup, bidStackLevels, stackType, orderType);
    }

    public void clearAskStack(final StackType stackType, final StackOrderType orderType) {

        clearStack(askStackGroup, askStackLevels, stackType, orderType);
    }

    private void clearStack(final StackGroup stackGroup, final LongMap<SymbolStackPriceLevel> stackLevels, final StackType stackType,
            final StackOrderType orderType) {

        if (null != stackGroup) {
            for (final LongMapNode<SymbolStackPriceLevel> stackPriceLevelNode : stackLevels) {

                final SymbolStackPriceLevel stackPriceLevel = stackPriceLevelNode.getValue();
                final StackLevel level = stackPriceLevel.getStackType(stackType);
                if (null != level) {
                    if (0 < level.getOrderTypeQty(orderType)) {
                        stackClient.addStackQty(SOURCE, stackGroup.getStackID(), stackGroup.getFillCount(), stackType, orderType,
                                level.getPullbackTicks(), -Long.MAX_VALUE);
                    }
                }
            }
            stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean adjustBidStackLevels(final int tickAdjustment) {

        return adjustStackLevels(bidStackGroup, tickAdjustment);
    }

    public boolean adjustAskStackLevels(final int tickAdjustment) {

        return adjustStackLevels(askStackGroup, tickAdjustment);
    }

    private boolean adjustStackLevels(final StackGroup stackGroup, final int tickAdjustment) {

        if (null != stackGroup) {

            for (final StackType stackType : StackType.values()) {
                stackClient.adjustStackLevels(SOURCE, stackGroup.getStackID(), stackType, tickAdjustment);
            }
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean clearBidStackPrice(final StackType stackType, final int tickOffset) {

        return clearStackPrice(bidStackGroup, bidStackLevels, stackType, BID_PRICE_MULTIPLIER, tickOffset);
    }

    public boolean clearAskStackPrice(final StackType stackType, final int tickOffset) {

        return clearStackPrice(askStackGroup, askStackLevels, stackType, ASK_PRICE_MULTIPLIER, tickOffset);
    }

    private boolean clearStackPrice(final StackGroup stackGroup, final LongMap<SymbolStackPriceLevel> stackLevels,
            final StackType stackType, final int priceMultiplier, final int tickOffset) {

        if (null != stackGroup) {
            final SymbolStackPriceLevel stackPriceLevel = stackLevels.get(tickOffset);
            if (null != stackPriceLevel) {
                final StackLevel level = stackPriceLevel.getStackType(stackType);
                if (null != level) {
                    for (final StackOrderType orderType : StackOrderType.values()) {
                        if (0 < level.getOrderTypeQty(orderType)) {
                            stackClient.addStackQty(SOURCE, stackGroup.getStackID(), stackGroup.getFillCount(), stackType, orderType,
                                    priceMultiplier * tickOffset, -Long.MAX_VALUE);
                        }
                    }
                }
            }
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean addBidStackQty(final StackType stackType, final StackOrderType orderType, final int tickOffset, final long qty) {

        if (null != bidStackGroup) {
            stackClient.addStackQty(SOURCE, bidStackGroup.getStackID(), bidStackGroup.getFillCount(), stackType, orderType,
                    BID_PRICE_MULTIPLIER * tickOffset, qty);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public boolean addAskStackQty(final StackType stackType, final StackOrderType orderType, final int tickOffset, final long qty) {

        if (null != askStackGroup) {
            stackClient.addStackQty(SOURCE, askStackGroup.getStackID(), askStackGroup.getFillCount(), stackType, orderType,
                    ASK_PRICE_MULTIPLIER * tickOffset, qty);
            return stackClient.batchComplete();
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    public void moveBidOrders(final StackType stackType, final int fromPrice, final int toPrice) {

        if (null != bidStackGroup) {

            final SymbolStackPriceLevel level = getBidPriceLevel(fromPrice);
            moveOrders(stackType, level, bidStackGroup, bidStackLevels, fromPrice, toPrice, BID_PRICE_MULTIPLIER);
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }

    }

    public void moveAskOrders(final StackType stackType, final int fromPrice, final int toPrice) {

        if (null != askStackGroup) {

            final SymbolStackPriceLevel level = getAskPriceLevel(fromPrice);
            moveOrders(stackType, level, askStackGroup, askStackLevels, fromPrice, toPrice, ASK_PRICE_MULTIPLIER);
        } else {
            throw new IllegalStateException("No stack for symbol.");
        }
    }

    private void moveOrders(final StackType stackType, final SymbolStackPriceLevel level, final StackGroup stackGroup,
            final LongMap<SymbolStackPriceLevel> stackLevels, final int fromPrice, final int toPrice, final int priceMultiplier) {

        if (null != level) {

            final StackLevel stackLevel = level.getStackType(stackType);

            if (null != stackLevel) {
                final Map<StackOrderType, Long> removedQties = new EnumMap<>(StackOrderType.class);

                for (final StackOrderType orderType : StackOrderType.values()) {

                    final long qty = stackLevel.getOrderTypeQty(orderType);
                    if (0 < qty) {
                        removedQties.put(orderType, qty);
                    }
                }

                clearStackPrice(stackGroup, stackLevels, stackType, priceMultiplier, fromPrice);

                for (final Map.Entry<StackOrderType, Long> removedQty : removedQties.entrySet()) {

                    final StackOrderType orderType = removedQty.getKey();
                    final long qty = removedQty.getValue();
                    stackClient.addStackQty(SOURCE, stackGroup.getStackID(), stackGroup.getFillCount(), stackType, orderType,
                            priceMultiplier * toPrice, qty);
                }

                stackClient.batchComplete();
            }
        }
    }

    public void startBidStrategy(final User user) {

        if (null != bidStackGroup) {
            stackClient.startStrategy(bidStackGroup.getSymbol(), BookSide.BID, user);
            for (final StackType stackType : StackType.values()) {
                stackClient.setStackEnabled(SOURCE, bidStackGroup.getStackID(), stackType, true);
            }
            stackClient.batchComplete();
        }
    }

    public void startAskStrategy(final User user) {

        if (null != askStackGroup) {
            stackClient.startStrategy(askStackGroup.getSymbol(), BookSide.ASK, user);
            for (final StackType stackType : StackType.values()) {
                stackClient.setStackEnabled(SOURCE, askStackGroup.getStackID(), stackType, true);
            }
            stackClient.batchComplete();
        }
    }

    public void stopBidStrategy() {

        if (null != bidStackGroup) {
            stackClient.stopStrategy(bidStackGroup.getSymbol(), BookSide.BID);
            stackClient.batchComplete();
            for (final StackType stackType : StackType.values()) {
                stackClient.setStackEnabled(SOURCE, bidStackGroup.getStackID(), stackType, false);
            }
            stackClient.batchComplete();
        }
    }

    public void stopAskStrategy() {

        if (null != askStackGroup) {
            stackClient.stopStrategy(askStackGroup.getSymbol(), BookSide.ASK);
            stackClient.batchComplete();
            for (final StackType stackType : StackType.values()) {
                stackClient.setStackEnabled(SOURCE, askStackGroup.getStackID(), stackType, false);
            }
            stackClient.batchComplete();
        }
    }
}
