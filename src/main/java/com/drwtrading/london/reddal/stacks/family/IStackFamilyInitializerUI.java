package com.drwtrading.london.reddal.stacks.family;

public interface IStackFamilyInitializerUI {

    public void addFamily(final String familyName, final boolean isAsylum, final String uiName);

    public void setChild(final String familyName, final String childSymbol, final double bidPriceOffset, final double bidQtyMultiplier,
            final double askPriceOffset, final double askQtyMultiplier, final int familyToChildRatio);

    public void setParentData(final String familyName, final String uiName, final String bidPriceOffset, final String askPriceOffset,
            final boolean bidPicardEnabled, final boolean bidQuoterEnabled, final boolean askPicardEnabled, final boolean askQuoterEnabled);

    public void setChildData(final String symbol, final String leanSymbol, final String nibblerName, final boolean isBidStrategyOn,
            final String bidInfo, final boolean isBidPicardEnabled, final boolean isBidQuoterEnabled, final boolean isAskStrategyOn,
            final String askInfo, final boolean isAskPicardEnabled, final boolean isAskQuoterEnabled);

}
