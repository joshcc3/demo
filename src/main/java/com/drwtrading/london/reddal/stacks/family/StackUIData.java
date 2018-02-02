package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.transport.cache.stack.IStackGroupUpdateCallback;
import com.drwtrading.london.eeif.stack.transport.data.stacks.Stack;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackGroup;
import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.InstType;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.Map;

class StackUIData implements IStackGroupUpdateCallback {

    private static final String NO_PRICE_OFFSET = "---";
    private static final StackType[] STACK_TYPES = StackType.values();

    public final String source;
    public final String symbol;
    public final InstrumentID instID;
    public final String leanSymbol;
    public final InstType leanInstType;

    private final DecimalFormat priceOffsetDF;

    private final Map<BookSide, StackGroup> stackGroups;

    private String selectedConfig;

    private String bidPriceOffsetBPS;
    private String askPriceOffsetBPS;

    StackUIData(final String source, final String symbol, final InstrumentID instID, final String leanSymbol, final InstType leanInstType) {

        this.source = source;
        this.symbol = symbol;
        this.instID = instID;
        this.leanSymbol = leanSymbol;
        this.leanInstType = leanInstType;

        this.priceOffsetDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 10);

        this.stackGroups = new EnumMap<>(BookSide.class);

        this.selectedConfig = StackConfigType.DEFAULT.name();

        this.bidPriceOffsetBPS = NO_PRICE_OFFSET;
        this.askPriceOffsetBPS = NO_PRICE_OFFSET;
    }

    public void setSelectedConfig(final StackConfigType selectedConfig) {
        this.selectedConfig = selectedConfig.name();
    }

    @Override
    public void stackGroupCreated(final StackGroup stackGroup) {

        stackGroups.put(stackGroup.getSide(), stackGroup);
        stackGroupUpdated(stackGroup);
    }

    @Override
    public void stackGroupUpdated(final StackGroup stackGroup) {

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

        boolean isContainingEnabledStack = false;
        double bestOffset = Integer.MIN_VALUE;
        for (final StackType stackType : STACK_TYPES) {

            final Stack stack = stackGroup.getStack(stackType);
            if (stack.isEnabled()) {
                final StackLevel bestLevel = stack.getFirstLevel();
                if (null != bestLevel) {
                    isContainingEnabledStack = true;
                    final double levelOffset = baseOffset - stackGroup.getStackAlignmentTickToBPS() * bestLevel.getPullbackTicks();
                    bestOffset = Math.max(bestOffset, levelOffset);
                }
            }
        }

        if (isContainingEnabledStack) {
            this.bidPriceOffsetBPS = priceOffsetDF.format(bestOffset);
        } else {
            this.bidPriceOffsetBPS = NO_PRICE_OFFSET;
        }
    }

    private void setBestAskOffset(final StackGroup stackGroup) {

        final double baseOffset = stackGroup.getPriceOffsetBPS();

        boolean isContainingEnabledStack = false;
        double bestOffset = Integer.MAX_VALUE;
        for (final StackType stackType : STACK_TYPES) {

            final Stack stack = stackGroup.getStack(stackType);
            if (stack.isEnabled()) {
                final StackLevel bestLevel = stack.getFirstLevel();
                if (null != bestLevel) {
                    isContainingEnabledStack = true;
                    final double levelOffset = baseOffset + stackGroup.getStackAlignmentTickToBPS() * bestLevel.getPullbackTicks();
                    bestOffset = Math.min(bestOffset, levelOffset);
                }
            }
        }

        if (isContainingEnabledStack) {
            this.askPriceOffsetBPS = priceOffsetDF.format(bestOffset);
        } else {
            this.askPriceOffsetBPS = NO_PRICE_OFFSET;
        }
    }

    @Override
    public void remoteFillNotification(final String source, final StackGroup stackGroup, final StackType stackType,
            final int maxPullbackTicks, final long qty) {
        // no-op
    }

    String getBidPriceOffsetBPS() {
        return bidPriceOffsetBPS;
    }

    String getAskPriceOffsetBPS() {
        return askPriceOffsetBPS;
    }

    String getSelectedConfigType() {
        return selectedConfig;
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

        this.selectedConfig = StackConfigType.DEFAULT.name();

        this.bidPriceOffsetBPS = NO_PRICE_OFFSET;
        this.askPriceOffsetBPS = NO_PRICE_OFFSET;
    }
}
