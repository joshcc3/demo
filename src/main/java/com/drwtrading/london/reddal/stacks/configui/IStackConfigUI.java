package com.drwtrading.london.reddal.stacks.configui;

public interface IStackConfigUI {

    public void removeAll(final String nibblerName);

    public void setRow(final String nibblerName, final long configGroupID, final String symbol, final int quoteMaxBookAgeMillis,
            final boolean quoteIsAuctionQuotingEnabled, final boolean quoteIsOnlyAuction, final int quoteAuctionTheoMaxBPSThrough,
            final boolean quoteIsAllowEmptyBook, final int quoteMaxJumpBPS, final int quoteBettermentQty, final int quoteBettermentTicks,
            final boolean quoteIsBettermentOppositeSide, final int oppositeSideBettermentTicks, final int fxMaxBookAgeMillis,
            final int fxMaxJumpBPS, final int leanMaxBookAgeMillis, final int leanMaxJumpBPS, final int leanRequiredQty,
            final int leanMaxPapaWeight, final String leanToQuoteRatio, final double leanPriceAdjustmentRaw,
            final boolean additiveIsEnabled, final long additiveMaxSignalAgeMillis, final int additiveMinRequiredBPS,
            final int additiveMaxBPS, final int planMinLevelQty, final int planMaxLevelQty, final int planLotSize, final int planMaxLevels,
            final int minPicardQty, final int maxOrdersPerLevel, final boolean isOnlySubmitBestLevel, final boolean isQuoteBettermentOn,
            final int modTicks, final byte quoteFlickerBufferPercent, final int quotePicardMaxBPSThrough, final byte maxPapaWeight,
            final int picardMaxPerSec, final int picardMaxPerMin, final int picardMaxPerHour, final int picardMaxPerDay);
}
