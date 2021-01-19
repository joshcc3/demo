package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.InstType;

public class StackFamilyChildRow {

    private final StackUIData uiData;

    private String bidInfo;
    private boolean isBidStrategyOn;
    private boolean isBidPicardEnabled;
    private boolean isBidQuoterEnabled;

    private String askInfo;
    private boolean isAskStrategyOn;
    private boolean isAskPicardEnabled;
    private boolean isAskQuoterEnabled;

    public StackFamilyChildRow(final StackUIData uiData) {

        this.uiData = uiData;

        this.bidInfo = "";
        this.askInfo = "";

        updateSnapshot(uiData);
    }

    public String getSymbol() {
        return uiData.symbol;
    }

    public String getSource() {
        return uiData.source;
    }

    public String getLeanSymbol() {
        return uiData.leanSymbol;
    }

    public InstType getLeanInstType() {
        return uiData.leanInstType;
    }

    public String getAdditiveSymbol() {
        return uiData.additiveSymbol;
    }

    public boolean updateSnapshot(final StackUIData uiData) {

        final String bidInfo = uiData.getRunningInfo(BookSide.BID);
        final boolean isBidStrategyOn = uiData.isStrategyOn(BookSide.BID);
        final boolean isBidPicardEnabled = uiData.isStackEnabled(BookSide.BID, StackType.PICARD);
        final boolean isBidQuoterEnabled = uiData.isStackEnabled(BookSide.BID, StackType.QUOTER);

        final String askInfo = uiData.getRunningInfo(BookSide.ASK);
        final boolean isAskStrategyOn = uiData.isStrategyOn(BookSide.ASK);
        final boolean isAskPicardEnabled = uiData.isStackEnabled(BookSide.ASK, StackType.PICARD);
        final boolean isAskQuoterEnabled = uiData.isStackEnabled(BookSide.ASK, StackType.QUOTER);

        final boolean hasChanged = this.isBidStrategyOn != isBidStrategyOn || this.isBidPicardEnabled != isBidPicardEnabled ||
                this.isBidQuoterEnabled != isBidQuoterEnabled || this.isAskStrategyOn != isAskStrategyOn ||
                this.isAskPicardEnabled != isAskPicardEnabled || this.isAskQuoterEnabled != isAskQuoterEnabled ||
                !this.bidInfo.equals(bidInfo) || !this.askInfo.equals(askInfo);

        if (hasChanged) {

            this.bidInfo = bidInfo;
            this.isBidStrategyOn = isBidStrategyOn;
            this.isBidPicardEnabled = isBidPicardEnabled;
            this.isBidQuoterEnabled = isBidQuoterEnabled;

            this.askInfo = askInfo;
            this.isAskStrategyOn = isAskStrategyOn;
            this.isAskPicardEnabled = isAskPicardEnabled;
            this.isAskQuoterEnabled = isAskQuoterEnabled;
        }
        return hasChanged;
    }

    public void sendRowState(final IStackFamilyInitializerUI view) {
        view.setChildData(uiData.symbol, uiData.leanSymbol, uiData.source, isBidStrategyOn, bidInfo, isBidPicardEnabled, isBidQuoterEnabled,
                isAskStrategyOn, askInfo, isAskPicardEnabled, isAskQuoterEnabled);
    }
}
