package com.drwtrading.london.reddal.stacks.configui;

import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;

import java.util.Collection;

public interface IStackConfigUI {

    public void removeAll();

    public void setConfigTypes(final Collection<String> configTypes);

    public void setRow(final long id, final String symbol, final StackConfigType configType, final int quoteMaxBookAgeMillis,
            final boolean quoteIsAuctionQuotingEnabled, final boolean quoteIsOnlyAuction, final int quoteAuctionTheoMaxTicksThrough,
            final int quoteMaxJumpBPS, final int quoteBettermentQty, final int quoteBettermentTicks, final int fxMaxBookAgeMillis,
            final int fxMaxJumpBPS, final int leanMaxBookAgeMillis, final int leanMaxJumpBPS, final int leanRequiredQty,
            final int leanMaxPapaWeight, final String leanToQuoteRatio, final int bidPlanMinLevelQty, final int bidPlanMaxLevelQty,
            final int bidPlanLotSize, final int bidPlanMaxLevels, final int bidMaxOrdersPerLevel, final boolean bidIsQuoteBettermentOn,
            final byte bidQuoteFlickerBufferPercent, final int bidQuotePicardMaxTicksThrough, final int bidPicardMaxPerSec,
            final int bidPicardMaxPerMin, final int bidPicardMaxPerHour, final int bidPicardMaxPerDay, final int askPlanMinLevelQty,
            final int askPlanMaxLevelQty, final int askPlanLotSize, final int askPlanMaxLevels, final int askMaxOrdersPerLevel,
            final boolean askIsQuoteBettermentOn, final byte askQuoteFlickerBufferPercent, final int askQuotePicardMaxTicksThrough,
            final int askPicardMaxPerSec, final int askPicardMaxPerMin, final int askPicardMaxPerHour, final int askPicardMaxPerDay);
}
