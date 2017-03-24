package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
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

    private String bidPriceOffsetBPS;
    private String askPriceOffsetBPS;

    StackUIData(final String familyName) {

        this.familyName = familyName;

        this.priceOffsetDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2, 10);

        this.enabledStacks = new EnumMap<>(BookSide.class);
        for (final BookSide side : BookSide.values()) {
            this.enabledStacks.put(side, EnumSet.noneOf(StackType.class));
        }

        this.selectedConfig = StackConfigType.DEFAULT.name();

        this.bidPriceOffsetBPS = NO_PRICE_OFFSET;
        this.askPriceOffsetBPS = NO_PRICE_OFFSET;
    }

    public void setSelectedConfig(final StackConfigType selectedConfig) {
        this.selectedConfig = selectedConfig.name();
    }

    public void setBidStacks(final double priceOffsetBPS) {
        this.bidPriceOffsetBPS = priceOffsetDF.format(priceOffsetBPS);
    }

    public void setAskStacks(final double priceOffsetBPS) {
        this.askPriceOffsetBPS = priceOffsetDF.format(priceOffsetBPS);
    }

    public void setStackEnabled(final BookSide side, final StackType stackType, final boolean enabled) {

        final Set<StackType> stacks = enabledStacks.get(side);
        if (enabled) {
            stacks.add(stackType);
        } else {
            stacks.remove(stackType);
        }
    }

    public String getBidPriceOffsetBPS() {
        return bidPriceOffsetBPS;
    }

    public String getAskPriceOffsetBPS() {
        return askPriceOffsetBPS;
    }

    public String getSelectedConfigType() {
        return selectedConfig;
    }

    public boolean isStackEnabled(final BookSide side, final StackType stackType) {

        return enabledStacks.get(side).contains(stackType);
    }
}
