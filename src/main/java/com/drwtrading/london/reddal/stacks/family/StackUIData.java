package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.transport.cache.stack.IStackGroupUpdateCallback;
import com.drwtrading.london.eeif.stack.transport.data.stacks.Stack;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.InstType;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;

public class StackUIData implements IStackGroupUpdateCallback {

    private static final ThreadLocal<DecimalFormat> priceOffsetDFTL =
            ThreadLocal.withInitial(() -> NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 10));

    private static final String NO_PRICE_OFFSET = "---";
    private static final StackType[] STACK_TYPES = StackType.values();

    public final String source;
    public final String symbol;
    public final InstrumentID instID;
    public final String leanSymbol;
    public final InstType leanInstType;
    public final String additiveSymbol;

    private final Map<BookSide, StackGroup> stackGroups;

    private String definedBidPriceOffsetBPS;
    private String definedAskPriceOffsetBPS;

    private String activeBidPriceOffsetBPS;
    private String activeAskPriceOffsetBPS;

    StackUIData(final String source, final String symbol, final InstrumentID instID, final String leanSymbol, final InstType leanInstType,
            final String additiveSymbol) {

        this.source = source;
        this.symbol = symbol;
        this.instID = instID;
        this.leanSymbol = leanSymbol;
        this.leanInstType = leanInstType;
        this.additiveSymbol = additiveSymbol;

        this.stackGroups = new EnumMap<>(BookSide.class);

        this.definedBidPriceOffsetBPS = NO_PRICE_OFFSET;
        this.definedAskPriceOffsetBPS = NO_PRICE_OFFSET;

        this.activeBidPriceOffsetBPS = NO_PRICE_OFFSET;
        this.activeAskPriceOffsetBPS = NO_PRICE_OFFSET;
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup) {

        stackGroups.put(stackGroup.getSide(), stackGroup);
        stackGroupUpdated(stackGroup, false);
    }

    @Override
    public void stackGroupUpdated(final StackGroup stackGroup, final boolean isCrossCheckRequired) {

        if (BookSide.BID == stackGroup.getSide()) {
            setBestBidOffset(stackGroup);
        } else {
            setBestAskOffset(stackGroup);
        }
    }

    @Override
    public void stackGroupInfoUpdated(final StackGroup stackGroup) {
        // no-op
    }

    private void setBestBidOffset(final StackGroup stackGroup) {

        final double baseOffset = stackGroup.getPriceOffsetBPS();

        boolean isStackActive = false;
        boolean isActiveStackDefined = false;
        boolean isStackDefined = false;

        double bestDefinedOffset = Integer.MIN_VALUE;
        double bestActiveStack = Integer.MIN_VALUE;

        for (final StackType stackType : STACK_TYPES) {

            final Stack stack = stackGroup.getStack(stackType);
            final StackLevel bestLevel = stack.getFirstLevel();

            isStackActive |= stack.isEnabled();

            if (null != bestLevel) {

                isStackDefined = true;
                final double levelOffset = baseOffset - stackGroup.getStackAlignmentTickToBPS() * bestLevel.getPullbackTicks();
                bestDefinedOffset = Math.max(bestDefinedOffset, levelOffset);

                if (stack.isEnabled()) {
                    isActiveStackDefined = true;
                    bestActiveStack = Math.max(bestActiveStack, levelOffset);
                }
            }
        }

        if (isActiveStackDefined) {

            this.activeBidPriceOffsetBPS = priceOffsetDFTL.get().format(bestActiveStack);
            this.definedBidPriceOffsetBPS = activeBidPriceOffsetBPS;

        } else {

            this.activeBidPriceOffsetBPS = NO_PRICE_OFFSET;

            if (!isStackActive && isStackDefined) {
                this.definedBidPriceOffsetBPS = priceOffsetDFTL.get().format(bestDefinedOffset);
            } else {
                this.definedBidPriceOffsetBPS = NO_PRICE_OFFSET;
            }
        }
    }

    private void setBestAskOffset(final StackGroup stackGroup) {

        final double baseOffset = stackGroup.getPriceOffsetBPS();

        boolean isStackActive = false;
        boolean isActiveStackDefined = false;
        boolean isStackDefined = false;

        double bestDefinedOffset = Integer.MAX_VALUE;
        double bestActiveStack = Integer.MAX_VALUE;

        for (final StackType stackType : STACK_TYPES) {

            final Stack stack = stackGroup.getStack(stackType);
            final StackLevel bestLevel = stack.getFirstLevel();

            isStackActive |= stack.isEnabled();

            if (null != bestLevel) {

                isStackDefined = true;
                final double levelOffset = baseOffset + stackGroup.getStackAlignmentTickToBPS() * bestLevel.getPullbackTicks();
                bestDefinedOffset = Math.min(bestDefinedOffset, levelOffset);

                if (stack.isEnabled()) {
                    isActiveStackDefined = true;
                    bestActiveStack = Math.min(bestActiveStack, levelOffset);
                }
            }
        }

        if (isActiveStackDefined) {

            this.activeAskPriceOffsetBPS = priceOffsetDFTL.get().format(bestActiveStack);
            this.definedAskPriceOffsetBPS = activeAskPriceOffsetBPS;

        } else {

            this.activeAskPriceOffsetBPS = NO_PRICE_OFFSET;

            if (!isStackActive && isStackDefined) {
                this.definedAskPriceOffsetBPS = priceOffsetDFTL.get().format(bestDefinedOffset);
            } else {
                this.definedAskPriceOffsetBPS = NO_PRICE_OFFSET;
            }
        }
    }

    @Override
    public void remoteFillNotification(final String source, final StackGroup stackGroup, final StackType stackType,
            final int maxPullbackTicks, final long qty) {
        // no-op
    }

    public String getDefinedBidPriceOffsetBPS() {
        return definedBidPriceOffsetBPS;
    }

    public String getDefinedAskPriceOffsetBPS() {
        return definedAskPriceOffsetBPS;
    }

    public String getActiveBidPriceOffsetBPS() {
        return activeBidPriceOffsetBPS;
    }

    public String getActiveAskPriceOffsetBPS() {
        return activeAskPriceOffsetBPS;
    }

    boolean isStackEnabled(final BookSide side, final StackType stackType) {

        final StackGroup group = stackGroups.get(side);
        return null != group && group.getStack(stackType).isEnabled();
    }

    boolean isStrategyOn(final BookSide side) {
        final StackGroup group = stackGroups.get(side);
        return null != group && group.getStackGroupInfo().isStrategyRunning();
    }

    String getRunningInfo(final BookSide side) {
        final StackGroup group = stackGroups.get(side);
        if (null == group) {
            return "NO INFO";
        } else {
            return group.getStackGroupInfo().getStrategyInfo();
        }
    }

    void clear() {

        this.stackGroups.clear();

        this.definedBidPriceOffsetBPS = NO_PRICE_OFFSET;
        this.definedAskPriceOffsetBPS = NO_PRICE_OFFSET;

        this.activeBidPriceOffsetBPS = NO_PRICE_OFFSET;
        this.activeAskPriceOffsetBPS = NO_PRICE_OFFSET;
    }
}
