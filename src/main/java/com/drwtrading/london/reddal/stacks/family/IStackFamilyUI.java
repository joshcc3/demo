package com.drwtrading.london.reddal.stacks.family;

import java.util.Collection;
import java.util.Map;

public interface IStackFamilyUI extends IStackFamilyInitializerUI {

    public void setGlobalOffset(final String globalOffset);

    public void setGlobalStackEnabled(final boolean isBidPicardEnabled, final boolean isBidQuoterEnabled, final boolean isAskQuoterEnabled,
            final boolean isAskPicardEnabled);

    public void setFilters(final Map<String, String> filters);

    public void clearFieldData(final String fieldID);

    public void setInstID(final String resultFieldID, final String isin, final String ccy, final String mic, final String instType);

    public void setFieldData(final String fieldID, final String text);

    public void removeAll(final String nibblerName);

    public void setCreateFamilyRow(final String symbol, final boolean isFamilyExists, final String foundFamilyName);

    public void addCreateChildRow(final String symbol, final boolean isChildAlreadyCreated, final Collection<String> nibblers,
            final String tradableNibbler, final Collection<String> instTypes, final String leanType, final String leanSymbol);

    public void removeChild(final String familyName, final String childSymbol);

    public void showChild(final String symbol);

    public void displayErrorMsg(final String text);

    public void openConfig(final String symbolList);

    public void offsetsSaved();

    public void offsetsLoaded();

    void displayInfoMsg(String text);

    void lazySymbolSubscribe(String symbol);

    void setFamilyName(String parentSymbol, String uiName);

    void sendUIVersionFormat(String versionNum);

    void sendInitializationParentData(String cachedFamilyData);

    void sendInitializationChildData(String cachedChildData);

}
