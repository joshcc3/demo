package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;

import java.text.DecimalFormat;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

class StackUIData {

    private static final String NO_PRICE_OFFSET = "---";

    public final String familyName;

    private final DecimalFormat priceOffsetDF;

    private final Map<BookSide, Set<StackType>> enabledStacks;

    private String selectedConfig;

    private String bidPriceOffset;
    private String askPriceOffset;

    StackUIData(final String familyName) {

        this.familyName = familyName;

        this.priceOffsetDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 4, 10);

        this.enabledStacks = new EnumMap<>(BookSide.class);
        for (final BookSide side : BookSide.values()) {
            this.enabledStacks.put(side, EnumSet.noneOf(StackType.class));
        }

        this.selectedConfig = null;

        this.bidPriceOffset = NO_PRICE_OFFSET;
        this.askPriceOffset = NO_PRICE_OFFSET;
    }

    public void setSelectedConfig(final StackConfigType selectedConfig) {
        this.selectedConfig = selectedConfig.name();
    }

    public void setBidStacks(final long priceOffset) {
        this.bidPriceOffset = formatPriceOffset(priceOffset);
    }

    public void setAskStacks(final long priceOffset) {
        this.askPriceOffset = formatPriceOffset(priceOffset);
    }

    public String formatPriceOffset(final long priceOffset) {
        final double decimalOffset = priceOffset / (double) Constants.NORMALISING_FACTOR;
        return priceOffsetDF.format(decimalOffset);
    }

    public void setStackEnabled(final BookSide side, final StackType stackType, final boolean enabled) {

        final Set<StackType> stacks = enabledStacks.get(side);
        if (enabled) {
            stacks.add(stackType);
        } else {
            stacks.remove(stackType);
        }
    }

    public String getBidPriceOffset() {

        return bidPriceOffset;
    }

    public String getAskPriceOffset() {
        return askPriceOffset;
    }

    public String getSelectedConfigType() {
        return selectedConfig;
    }

    public boolean isStackEnabled(final BookSide side, final StackType stackType) {

        return enabledStacks.get(side).contains(stackType);
    }
}
