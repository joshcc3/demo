package com.drwtrading.london.reddal.stacks.family;

// TODO : split istackfamilyui into initUI and other
public class StackFamilyUIDataCompressor implements IStackFamilyInitializerUI {

    private static final String DELIMITER = new String(new byte[]{0x00});
    private final StringBuilder sb;

    public StackFamilyUIDataCompressor(final int size) {
        sb = new StringBuilder(size);
    }

    @Override
    public void addFamily(final String familyName, final boolean isAsylum, final String uiName, final boolean unhide) {
        sb.append("addFamily");
        sb.append(DELIMITER);
        sb.append(familyName);
        sb.append(DELIMITER);
        sb.append(isAsylum ? 1 : 0);
        sb.append(DELIMITER);
        sb.append(uiName);
        sb.append(DELIMITER);
    }

    @Override
    public void setChild(final String familyName, final String childSymbol, final double bidPriceOffset, final double bidQtyMultiplier,
            final double askPriceOffset, final double askQtyMultiplier, final int familyToChildRatio) {
        sb.append("setChild");
        sb.append(DELIMITER);
        sb.append(familyName);
        sb.append(DELIMITER);
        sb.append(childSymbol);
        sb.append(DELIMITER);
        sb.append(bidPriceOffset);
        sb.append(DELIMITER);
        sb.append(bidQtyMultiplier);
        sb.append(DELIMITER);
        sb.append(askPriceOffset);
        sb.append(DELIMITER);
        sb.append(askQtyMultiplier);
        sb.append(DELIMITER);
        sb.append(familyToChildRatio);
        sb.append(DELIMITER);
    }

    @Override
    public void setParentData(final String familyName, final String uiName, final String bidPriceOffset, final String askPriceOffset,
            final boolean bidPicardEnabled, final boolean bidQuoterEnabled, final boolean askPicardEnabled,
            final boolean askQuoterEnabled) {
        final int bidPicardEnabledBit = bidPicardEnabled ? 1 : 0;
        final int bidQuoterEnabledBit = bidQuoterEnabled ? 1 : 0;
        final int askPicardEnabledBit = askPicardEnabled ? 1 : 0;
        final int askQuoterEnabledBit = askQuoterEnabled ? 1 : 0;
        final int enabledState = bidPicardEnabledBit | (bidQuoterEnabledBit << 1) | (askPicardEnabledBit << 2) | (askQuoterEnabledBit << 3);
        sb.append("setParentData");
        sb.append(DELIMITER);
        sb.append(familyName);
        sb.append(DELIMITER);
        sb.append(uiName);
        sb.append(DELIMITER);
        sb.append(bidPriceOffset);
        sb.append(DELIMITER);
        sb.append(askPriceOffset);
        sb.append(DELIMITER);
        sb.append(enabledState);
        sb.append(DELIMITER);
    }

    @Override
    public void setChildData(final String symbol, final String leanSymbol, final String nibblerName, final boolean isBidStrategyOn,
            final String bidInfo, final boolean isBidPicardEnabled, final boolean isBidQuoterEnabled, final boolean isAskStrategyOn,
            final String askInfo, final boolean isAskPicardEnabled, final boolean isAskQuoterEnabled) {

        final int isBidStrategyOnBit = isBidStrategyOn ? 1 : 0;
        final int isBidPicardEnabledBit = isBidPicardEnabled ? 1 : 0;
        final int isBidQuoterEnabledBit = isBidQuoterEnabled ? 1 : 0;
        final int isAskStrategyOnBit = isAskStrategyOn ? 1 : 0;
        final int isAskPicardEnabledBit = isAskPicardEnabled ? 1 : 0;
        final int isAskQuoterEnabledBit = isAskQuoterEnabled ? 1 : 0;

        final int quoterStateBitSet =
                isBidStrategyOnBit | (isBidPicardEnabledBit << 1) | (isBidQuoterEnabledBit << 2) | (isAskStrategyOnBit << 3) |
                        (isAskPicardEnabledBit << 4) | (isAskQuoterEnabledBit << 5);
        sb.append("setChildData");
        sb.append(DELIMITER);
        sb.append(symbol);
        sb.append(DELIMITER);
        sb.append(leanSymbol);
        sb.append(DELIMITER);
        sb.append(nibblerName);
        sb.append(DELIMITER);
        sb.append(bidInfo);
        sb.append(DELIMITER);
        sb.append(askInfo);
        sb.append(DELIMITER);
        sb.append(quoterStateBitSet);
        sb.append(DELIMITER);

    }

    public String batchComplete() {
        if (sb.length() > 1) {
            sb.setLength(sb.length() - 1);
            final String result = sb.toString();
            sb.setLength(0);
            return result;
        } else {
            return "";
        }
    }
}
