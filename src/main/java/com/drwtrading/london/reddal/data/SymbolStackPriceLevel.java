package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackOrderType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;

public class SymbolStackPriceLevel {

    private static final int STACK_TYPES = StackType.values().length;

    private final StackLevel[] stackTypes;

    private long totalQty;
    private String totalQtyString;

    private boolean isQuoterPresent;
    private boolean isPicardPresent;

    private boolean isOneShotPresent;
    private boolean isAutoMangePresent;
    private boolean isRefreshablePresent;

    public SymbolStackPriceLevel() {

        this.stackTypes = new StackLevel[STACK_TYPES];

        this.totalQty = 0;
    }

    public void setStackLevel(final StackType stackType, final StackLevel stackLevel) {

        final StackLevel oldLevel = stackTypes[stackType.ordinal()];
        if (null != oldLevel) {
            totalQty -= oldLevel.getRemainingQty();
        }

        stackTypes[stackType.ordinal()] = stackLevel;
        this.totalQty += stackLevel.getRemainingQty();

        this.totalQtyString = Long.toString(totalQty);

        switch (stackType) {
            case PICARD: {
                isPicardPresent = 0 < stackLevel.getRemainingQty();
                break;
            }
            case QUOTER: {
                isQuoterPresent = 0 < stackLevel.getRemainingQty();
                break;
            }
        }

        isOneShotPresent |= 0 < stackLevel.getOrderTypeQty(StackOrderType.ONE_SHOT);
        isAutoMangePresent |= 0 < stackLevel.getOrderTypeQty(StackOrderType.AUTO_MANAGE);
        isRefreshablePresent |= 0 < stackLevel.getOrderTypeQty(StackOrderType.REFRESHABLE);
    }

    public long getTotalQty() {
        return totalQty;
    }

    public String getFormattedTotalQty() {
        return totalQtyString;
    }

    public boolean isQuoterPresent() {
        return isQuoterPresent;
    }

    public boolean isPicardPresent() {
        return isPicardPresent;
    }

    public boolean isOneShotPresent() {
        return isOneShotPresent;
    }

    public boolean isAutoMangePresent() {
        return isAutoMangePresent;
    }

    public boolean isRefreshablePresent() {
        return isRefreshablePresent;
    }

    public StackLevel getStackType(final StackType stackType) {
        return stackTypes[stackType.ordinal()];
    }
}
