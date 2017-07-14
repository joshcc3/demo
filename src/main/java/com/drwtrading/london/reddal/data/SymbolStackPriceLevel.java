package com.drwtrading.london.reddal.data;

import com.drwtrading.london.eeif.stack.transport.data.stacks.StackLevel;
import com.drwtrading.london.eeif.stack.transport.data.types.StackOrderType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;

public class SymbolStackPriceLevel {

    private static final int STACK_TYPES = StackType.values().length;

    private final StackLevel[] stackTypes;

    private final String[] totalQtyString;

    public SymbolStackPriceLevel() {

        this.stackTypes = new StackLevel[STACK_TYPES];
        this.totalQtyString = new String[STACK_TYPES];
    }

    public void setStackLevel(final StackType stackType, final StackLevel stackLevel) {

        stackTypes[stackType.ordinal()] = stackLevel;

        final String formattedQty = Long.toString(stackLevel.getRemainingQty());
        totalQtyString[stackType.ordinal()] = formattedQty;
    }

    public String getFormattedTotalQty(final StackType stackType) {
        return totalQtyString[stackType.ordinal()];
    }

    public boolean isStackPresent(final StackType stackType) {
        final StackLevel stackLevel = stackTypes[stackType.ordinal()];
        return null != stackLevel && 0 < stackLevel.getRemainingQty();
    }

    public boolean isOrderTypePresent(final StackType stackType, final StackOrderType orderType) {
        final StackLevel stackLevel = stackTypes[stackType.ordinal()];
        return null != stackLevel && 0 < stackLevel.getOrderTypeQty(orderType);
    }

    public StackLevel getStackType(final StackType stackType) {
        return stackTypes[stackType.ordinal()];
    }
}
