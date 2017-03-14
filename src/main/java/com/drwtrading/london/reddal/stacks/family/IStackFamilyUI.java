package com.drwtrading.london.reddal.stacks.family;

public interface IStackFamilyUI {

    public void clearFieldData(final String fieldID);

    public void setInstID(final String isin, final String ccy, final String mic, final String instType);

    public void setFieldData(final String fieldID, final String text);

    public void removeAll(final String nibblerName);

    public void addFamily(final String familyName);

    public void setChild(final String familyName, final String childSymbol, final double bidPriceOffset, final double bidQtyMultiplier,
            final double askPriceOffset, final double askQtyMultiplier);

    public void displayErrorMsg(final String text);

    void setParentData(final String familyName, final String bidPriceOffset, final String askPriceOffset, final String selectedConfigType,
            final boolean bidPicardEnabled, final boolean bidQuoterEnabled, final boolean askPicardEnabled, final boolean askQuoterEnabled);
}
