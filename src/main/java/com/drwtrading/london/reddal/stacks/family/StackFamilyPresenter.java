package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.transport.cache.families.IStackRelationshipListener;
import com.drwtrading.london.eeif.stack.transport.data.config.StackAdditiveConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.config.StackFXConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackLeanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackPlanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackQuoteConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackStrategyConfig;
import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.ExpiryPeriod;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.FutureExpiryCalc;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
import com.drwtrading.london.reddal.stacks.strategiesUI.StackStrategiesPresenter;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.collect.Lists;
import org.jetlang.channels.Publisher;

import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class StackFamilyPresenter implements IStackRelationshipListener {

    private static final Collection<String> ALLOWED_INST_TYPES =
            StackStrategiesPresenter.ALLOWED_INST_TYPES;

    private static final String SOURCE_UI = "FAMILY_ADMIN_UI";

    private static final String MD_SOURCE_FILTER_GROUP = "Trading Venue";
    private static final Pattern FILTER_SPLITTER = Pattern.compile("\\|");

    private static final String EXPIRY_FILTER_GROUP = "Expiries";
    private static final String EXPIRY_FRONT_MONTH_FILTER = "Front months";
    private static final String EXPIRY_BACK_MONTH_FILTER = "Back months";

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;
    private final SpreadContractSetGenerator contractSetGenerator;

    private final WebSocketViews<IStackFamilyUI> views;
    private final Map<String, HashSet<IStackFamilyUI>> userViews;

    private final FutureExpiryCalc expiryCalc;

    private final Map<String, NavigableMap<String, StackUIRelationship>> families;
    private final Map<String, StackUIData> parentData;
    private final Map<String, String> childrenToFamily;
    private final Map<String, StackUIData> childData;

    private final Map<String, SearchResult> searchResults;
    private final Map<String, LinkedHashSet<String>> fungibleInsts;

    private final Map<String, StackClientHandler> nibblerClients;
    private final Map<String, EnumMap<StackConfigType, StackConfigGroup>> stackConfigs;

    private final Map<String, String> filterGroups;
    private final Map<String, StackChildFilter> filters;

    private StackCommunityManager communityManager;

    public StackFamilyPresenter(final FiberBuilder logFiber, final UILogger uiLogger,
            final SpreadContractSetGenerator contractSetGenerator) {

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;
        this.contractSetGenerator = contractSetGenerator;

        this.views = WebSocketViews.create(IStackFamilyUI.class, this);
        this.userViews = new HashMap<>();

        this.expiryCalc = new FutureExpiryCalc(0);

        this.families = new HashMap<>();
        this.parentData = new HashMap<>();
        this.childrenToFamily = new HashMap<>();
        this.childData = new HashMap<>();

        this.searchResults = new HashMap<>();
        this.fungibleInsts = new HashMap<>();

        this.nibblerClients = new TreeMap<>();
        this.stackConfigs = new HashMap<>();

        this.filterGroups = new HashMap<>();
        this.filters = new HashMap<>();
    }

    public void setCommunityManager(final StackCommunityManager communityManager) {
        this.communityManager = communityManager;
    }

    public void setStrategyClient(final String nibblerName, final StackClientHandler cache) {
        this.nibblerClients.put(nibblerName, cache);
    }

    public void setFilter(final StackChildFilter newFilter) {

        filters.put(newFilter.filterName, newFilter);
        filterGroups.put(newFilter.filterName, newFilter.groupName);

        views.all().setFilters(filterGroups);
    }

    private void updateSymbolFilters(final String symbol) {

        final SearchResult searchResult = searchResults.get(symbol);
        if (null != searchResult && (InstType.ETF == searchResult.instType || InstType.FUTURE == searchResult.instType) &&
                childData.containsKey(symbol)) {

            final String filterName = searchResult.mdSource.name();
            final StackChildFilter filter = getFilter(filterName.trim(), MD_SOURCE_FILTER_GROUP);
            filter.addSymbol(symbol);

            if (InstType.FUTURE == searchResult.instType) {

                final FutureConstant future = FutureConstant.getFutureFromSymbol(symbol);

                if (symbol.equals(expiryCalc.getFutureCode(future))) {

                    final StackChildFilter frontMonths = getFilter(EXPIRY_FRONT_MONTH_FILTER, EXPIRY_FILTER_GROUP);
                    frontMonths.addSymbol(symbol);

                } else if (symbol.equals(expiryCalc.getFutureCode(future, 1))) {

                    final StackChildFilter backMonths = getFilter(EXPIRY_BACK_MONTH_FILTER, EXPIRY_FILTER_GROUP);
                    backMonths.addSymbol(symbol);
                }
            }
        }
    }

    private StackChildFilter getFilter(final String filterName, final String filterGroup) {

        final StackChildFilter filter = filters.get(filterName);
        if (null == filter) {

            final StackChildFilter result = new StackChildFilter(filterGroup, filterName);

            filters.put(filterName, result);
            filterGroups.put(filterName, filterGroup);
            views.all().setFilters(filterGroups);

            return result;
        } else {
            return filter;
        }
    }

    private Set<String> getFilteredSymbols(final String filters) {

        final String[] filterSet = FILTER_SPLITTER.split(filters);

        final Map<String, HashSet<String>> unionSets = new HashMap<>();

        for (final String filterName : filterSet) {

            final StackChildFilter filter = this.filters.get(filterName.trim());
            if (null != filter) {
                final Set<String> groupInsts = MapUtils.getMappedSet(unionSets, filter.groupName);
                groupInsts.addAll(filter.symbols);
            } else {
                unionSets.put("empty_set", new HashSet<>());
            }
        }

        final Set<String> result = new HashSet<>(childrenToFamily.keySet());
        result.removeAll(families.keySet());
        result.retainAll(childData.keySet());

        for (final Set<String> filterGroup : unionSets.values()) {
            result.retainAll(filterGroup);
        }

        return result;
    }

    void addFamily(final String familyName) {

        families.put(familyName, new TreeMap<>());
        views.all().addFamily(familyName);
    }

    void addFamilyUIData(final StackUIData uiData) {

        parentData.put(uiData.symbol, uiData);
        updateFamilyUIData(views.all(), uiData);
    }

    void updateFamilyUIData(final StackUIData uiData) {
        updateFamilyUIData(views.all(), uiData);
    }

    private static void updateFamilyUIData(final IStackFamilyUI view, final StackUIData uiData) {

        view.setParentData(uiData.symbol, uiData.getBidPriceOffsetBPS(), uiData.getAskPriceOffsetBPS(), uiData.getSelectedConfigType(),
                uiData.isStackEnabled(BookSide.BID, StackType.PICARD), uiData.isStackEnabled(BookSide.BID, StackType.QUOTER),
                uiData.isStackEnabled(BookSide.ASK, StackType.PICARD), uiData.isStackEnabled(BookSide.ASK, StackType.QUOTER));
    }

    void addChildUIData(final StackUIData uiData) {

        childData.put(uiData.symbol, uiData);
        updateChildUIData(views.all(), uiData);

        updateSymbolFilters(uiData.symbol);
    }

    void updateChildUIData(final StackUIData uiData) {
        updateChildUIData(views.all(), uiData);
    }

    private static void updateChildUIData(final IStackFamilyUI view, final StackUIData uiData) {

        view.setChildData(uiData.symbol, uiData.source, uiData.getSelectedConfigType(), uiData.isStrategyOn(BookSide.BID),
                uiData.getRunningInfo(BookSide.BID), uiData.isStackEnabled(BookSide.BID, StackType.PICARD),
                uiData.isStackEnabled(BookSide.BID, StackType.QUOTER), uiData.isStrategyOn(BookSide.ASK),
                uiData.getRunningInfo(BookSide.ASK), uiData.isStackEnabled(BookSide.ASK, StackType.PICARD),
                uiData.isStackEnabled(BookSide.ASK, StackType.QUOTER));
    }

    void setConfig(final StackConfigGroup stackConfig) {

        final Map<StackConfigType, StackConfigGroup> stackTypeConfigs =
                MapUtils.getMappedEnumMap(stackConfigs, stackConfig.getSymbol(), StackConfigType.class);
        stackTypeConfigs.put(stackConfig.configType, stackConfig);
    }

    @Override
    public boolean updateRelationship(final String source, final long relationshipID, final String childSymbol, final String parentSymbol,
            final double bidPriceOffset, final double bidQtyMultiplier, final double askPriceOffset, final double askQtyMultiplier,
            final int familyToChildRatio) {

        for (final Map<String, StackUIRelationship> children : families.values()) {
            children.remove(childSymbol);
        }

        final Map<String, StackUIRelationship> familyChildren = MapUtils.getNavigableMap(families, parentSymbol);

        final StackUIRelationship newRelationship =
                new StackUIRelationship(childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier,
                        familyToChildRatio);
        familyChildren.put(childSymbol, newRelationship);

        childrenToFamily.put(childSymbol, parentSymbol);

        views.all().setChild(parentSymbol, childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier,
                familyToChildRatio);

        contractSetGenerator.setParentStack(childSymbol, parentSymbol);
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }

    public void setSearchResult(final SearchResult searchResult) {

        searchResults.put(searchResult.symbol, searchResult);
        MapUtils.getMappedLinkedSet(fungibleInsts, searchResult.instID.isin).add(searchResult.symbol);

        updateSymbolFilters(searchResult.symbol);
    }

    public void symbolSelected(final SymbolSelection symbolSelection) {

        final Set<IStackFamilyUI> views = userViews.get(symbolSelection.username);
        if (null != views) {
            for (final IStackFamilyUI view : views) {

                view.showChild(symbolSelection.symbol);
            }
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketDisconnected) {

            final IStackFamilyUI oldView = views.unregister((WebSocketDisconnected) webMsg);
            userViews.get(webMsg.getClient().getUserName()).remove(oldView);

        } else if (webMsg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) webMsg);
        }
    }

    private void inboundData(final WebSocketInboundData msg) {

        logFiber.execute(() -> uiLogger.write("StackConfig", msg));

        final String data = msg.getData();
        if ("subscribe".equals(data)) {
            addUI(msg.getClient().getUserName(), msg.getOutboundChannel());
        } else {
            views.invoke(msg);
        }
    }

    private void addUI(final String username, final Publisher<WebSocketOutboundData> channel) {

        final IStackFamilyUI newView = views.get(channel);
        MapUtils.getMappedSet(userViews, username).add(newView);

        newView.setFilters(filterGroups);

        for (final Map.Entry<String, NavigableMap<String, StackUIRelationship>> family : families.entrySet()) {

            final String familyName = family.getKey();
            newView.addFamily(familyName);

            for (final StackUIRelationship child : family.getValue().values()) {

                newView.setChild(familyName, child.childSymbol, child.bidPriceOffsetBPS, child.bidQtyMultiplier, child.askPriceOffsetBPS,
                        child.askQtyMultiplier, child.familyToChildRatio);
            }
        }

        for (final StackUIData uiData : parentData.values()) {
            updateFamilyUIData(newView, uiData);
        }

        for (final StackUIData uiData : childData.values()) {
            updateChildUIData(newView, uiData);
        }
    }

    @FromWebSocketView
    public void refreshAllParents(final WebSocketInboundData data) {

        communityManager.reestablishParentalRule();
    }

    @FromWebSocketView
    public void cleanAllParents(final WebSocketInboundData data) {
        communityManager.cleanParentStacks(SOURCE_UI);
    }

    @FromWebSocketView
    public void checkFamilyInst(final String family, final String resultFieldID, final WebSocketInboundData data) {

        final IStackFamilyUI ui = views.get(data.getOutboundChannel());

        final SearchResult searchResult = searchResults.get(family);

        if (null == searchResult) {
            ui.clearFieldData(resultFieldID);
        } else {
            final InstrumentID instID = searchResult.instID;
            ui.setInstID(resultFieldID, instID.isin, instID.ccy.name(), instID.mic.name(), searchResult.instType.name());
        }
    }

    @FromWebSocketView
    public void findFamilyMembers(final String symbol, final WebSocketInboundData data) {

        final SearchResult searchResult = searchResults.get(symbol);
        if (null != searchResult) {

            final IStackFamilyUI ui = views.get(data.getOutboundChannel());
            final String family = getFamilyName(searchResult);
            final boolean isFamilyExists = families.containsKey(family);
            ui.setCreateFamilyRow(symbol, isFamilyExists);

            if (InstType.FUTURE == searchResult.instType) {

                final FutureConstant future = FutureConstant.getFutureFromSymbol(symbol);
                if (null != future) {
                    for (int i = 0; i < 3; ++i) {

                        final String expirySymbol = expiryCalc.getFutureCode(future, i);
                        final SearchResult expirySearchResult = searchResults.get(expirySymbol);

                        if (null != expirySearchResult) {

                            final boolean isChildAlreadyCreated = this.childrenToFamily.containsKey(expirySymbol);
                            ui.addCreateChildRow(expirySymbol, isChildAlreadyCreated, nibblerClients.keySet(), ALLOWED_INST_TYPES,
                                    InstType.INDEX.name(), expirySymbol);
                        }
                    }
                }
            } else {
                final Set<String> children = fungibleInsts.get(searchResult.instID.isin);
                for (final String childSymbol : children) {
                    final boolean isChildAlreadyCreated = this.childrenToFamily.containsKey(childSymbol);
                    ui.addCreateChildRow(childSymbol, isChildAlreadyCreated, nibblerClients.keySet(), ALLOWED_INST_TYPES,
                            InstType.INDEX.name(), childSymbol);
                }
            }
        }
    }

    @FromWebSocketView
    public void checkFamilyExists(final String family, final String resultFieldID, final WebSocketInboundData data) {

        final IStackFamilyUI ui = views.get(data.getOutboundChannel());

        if (families.containsKey(family)) {

            ui.setFieldData(resultFieldID, "Family found.");
        } else {

            ui.clearFieldData(resultFieldID);
        }
    }

    @FromWebSocketView
    public void createFamily(final String symbol, final WebSocketInboundData data) {

        final SearchResult searchResult = searchResults.get(symbol);
        if (null != searchResult) {

            final String family = getFamilyName(searchResult);

            final InstrumentID instID = searchResult.instID;
            final InstType instType = searchResult.instType;

            communityManager.createFamily(SOURCE_UI, family, instID, instType);
        }
    }

    private static String getFamilyName(final SearchResult searchResult) {

        switch (searchResult.instType) {
            case FUTURE: {
                return FutureConstant.getFutureFromSymbol(searchResult.symbol).name();
            }
            case FUTURE_SPREAD: {
                final String frontMonth = searchResult.symbol.split("-")[0];
                return FutureConstant.getFutureFromSymbol(frontMonth).name();
            }
            default: {
                return searchResult.symbol.split(" ")[0];
            }
        }
    }

    @FromWebSocketView
    public void checkChildExists(final String child, final String resultFieldID, final WebSocketInboundData data) {

        final IStackFamilyUI ui = views.get(data.getOutboundChannel());

        if (childrenToFamily.containsKey(child)) {

            ui.setFieldData(resultFieldID, "Child available.");
        } else {

            ui.clearFieldData(resultFieldID);
        }
    }

    @FromWebSocketView
    public void createChildStack(final String nibblerName, final String quoteSymbol, final String leanInstrumentType,
            final String leanSymbol, final WebSocketInboundData data) {

        final StackClientHandler strategyClient = nibblerClients.get(nibblerName);
        if (null != strategyClient) {

            final InstrumentID quoteInstId = searchResults.get(quoteSymbol).instID;
            final InstType leanInstType = InstType.getInstType(leanInstrumentType);
            final InstrumentID leanInstID = searchResults.get(leanSymbol).instID;

            if (null != quoteInstId && null != leanInstType && null != leanInstID) {
                strategyClient.createStrategy(quoteSymbol, quoteInstId, leanInstType, leanSymbol, leanInstID, "");
                strategyClient.batchComplete();
            }
        }
    }

    @FromWebSocketView
    public void createMonthlyFutures(final WebSocketInboundData data) {
        createFutures(ExpiryPeriod.MONTHLY);
    }

    @FromWebSocketView
    public void rollMonthlyFutures(final WebSocketInboundData data) {
        rollFutures(ExpiryPeriod.MONTHLY);
    }

    @FromWebSocketView
    public void createQuaterlyFutures(final WebSocketInboundData data) {
        createFutures(ExpiryPeriod.QUARTERLY);
    }

    @FromWebSocketView
    public void rollQuaterlyFutures(final WebSocketInboundData data) {
        rollFutures(ExpiryPeriod.QUARTERLY);
    }

    private void createFutures(final ExpiryPeriod expiryPeriod) {

        for (final FutureConstant future : FutureConstant.values()) {

            if (expiryPeriod == future.expiryPeriod) {

                final String frontMonthSymbol = expiryCalc.getFutureCode(future, 0);
                final StackUIData frontMonthData = childData.get(frontMonthSymbol);

                final String backMonthSymbol = expiryCalc.getFutureCode(future, 1);
                final SearchResult backMonthSearchResult = searchResults.get(backMonthSymbol);
                final StackUIData backMonthData = childData.get(backMonthSymbol);

                if (null != frontMonthData && null == backMonthData && null != backMonthSearchResult) {

                    final StackClientHandler strategyClient = nibblerClients.get(frontMonthData.source);

                    if (null != strategyClient) {
                        strategyClient.createStrategy(backMonthSymbol, backMonthSearchResult.instID, InstType.INDEX, backMonthSymbol,
                                backMonthSearchResult.instID, "");
                        strategyClient.batchComplete();
                    }
                }
            }
        }
    }

    private void rollFutures(final ExpiryPeriod expiryPeriod) {

        for (final FutureConstant future : FutureConstant.values()) {

            if (expiryPeriod == future.expiryPeriod) {

                final String fromSymbol = expiryCalc.getFutureCode(future, 0);
                final String toSymbol = expiryCalc.getFutureCode(future, 1);

                copyChildSetup(fromSymbol, toSymbol, null);
            }
        }
    }

    @FromWebSocketView
    public void copyChildSetup(final String fromSymbol, final String toSymbol, final WebSocketInboundData data) {

        final String family = childrenToFamily.get(toSymbol);
        communityManager.stopChild(family, toSymbol, BookSide.BID);
        communityManager.stopChild(family, toSymbol, BookSide.ASK);

        final Map<StackConfigType, StackConfigGroup> fromConfigs = stackConfigs.get(fromSymbol);
        final Map<StackConfigType, StackConfigGroup> toConfigs = stackConfigs.get(toSymbol);
        final StackUIData toChildUIData = childData.get(fromSymbol);

        if (null != fromConfigs && null != toConfigs) {

            communityManager.copyChildStacks(SOURCE_UI, fromSymbol, toSymbol);
            final StackClientHandler configClient = nibblerClients.get(toChildUIData.source);

            for (final StackConfigType configType : StackConfigType.values()) {

                final StackConfigGroup fromConfig = fromConfigs.get(configType);
                final StackConfigGroup toConfig = toConfigs.get(configType);

                final StackQuoteConfig quoteConfig = fromConfig.quoteConfig;
                configClient.quoteConfigUpdated(SOURCE_UI, toConfig.configGroupID, quoteConfig.getMaxBookAgeMillis(),
                        quoteConfig.isAuctionQuotingEnabled(), quoteConfig.isOnlyAuctionQuoting(),
                        quoteConfig.getAuctionTheoMaxTicksThrough(), quoteConfig.getMaxJumpBPS(), quoteConfig.getBettermentQty(),
                        quoteConfig.getBettermentTicks());

                final StackFXConfig fxConfig = fromConfig.fxConfig;
                configClient.fxConfigUpdated(SOURCE_UI, toConfig.configGroupID, fxConfig.getMaxBookAgeMillis(), fxConfig.getMaxJumpBPS());

                final StackLeanConfig leanConfig = fromConfig.leanConfig;
                configClient.leanConfigUpdated(SOURCE_UI, toConfig.configGroupID, leanConfig.getMaxBookAgeMillis(),
                        leanConfig.getMaxJumpBPS(), leanConfig.getRequiredQty(), leanConfig.getMaxPapaWeight(),
                        leanConfig.getLeanToQuoteRatio(), leanConfig.getPriceAdjustment());

                final StackAdditiveConfig additiveConfig = fromConfig.additiveConfig;
                configClient.additiveConfigUpdated(SOURCE_UI, toConfig.configGroupID, additiveConfig.getMaxSignalAgeMillis(),
                        additiveConfig.isEnabled(), additiveConfig.getMinRequiredBPS(), additiveConfig.getMaxBPS());

                final StackPlanConfig bidPlanConfig = fromConfig.bidPlanConfig;
                configClient.planConfigUpdated(SOURCE_UI, toConfig.configGroupID, BookSide.BID, bidPlanConfig.getMinLevelQty(),
                        bidPlanConfig.getMaxLevelQty(), bidPlanConfig.getLotSize(), bidPlanConfig.getMaxLevels(),
                        bidPlanConfig.getMinPicardQty());

                final StackStrategyConfig bidStratConfig = fromConfig.bidStrategyConfig;
                configClient.strategyConfigUpdated(SOURCE_UI, toConfig.configGroupID, BookSide.BID, bidStratConfig.getMaxOrdersPerLevel(),
                        bidStratConfig.isOnlySubmitBestLevel(), bidStratConfig.isQuoteBettermentOn(), bidStratConfig.getModTicks(),
                        bidStratConfig.getQuoteFlickerBufferPercent(), bidStratConfig.getQuotePicardMaxTicksThrough(),
                        bidStratConfig.getPicardMaxPerSec(), bidStratConfig.getPicardMaxPerMin(), bidStratConfig.getPicardMaxPerHour(),
                        bidStratConfig.getPicardMaxPerDay());

                final StackPlanConfig askPlanConfig = fromConfig.askPlanConfig;
                configClient.planConfigUpdated(SOURCE_UI, toConfig.configGroupID, BookSide.ASK, askPlanConfig.getMinLevelQty(),
                        askPlanConfig.getMaxLevelQty(), askPlanConfig.getLotSize(), askPlanConfig.getMaxLevels(),
                        askPlanConfig.getMinPicardQty());

                final StackStrategyConfig askStratConfig = fromConfig.askStrategyConfig;
                configClient.strategyConfigUpdated(SOURCE_UI, toConfig.configGroupID, BookSide.ASK, askStratConfig.getMaxOrdersPerLevel(),
                        askStratConfig.isOnlySubmitBestLevel(), askStratConfig.isQuoteBettermentOn(), askStratConfig.getModTicks(),
                        askStratConfig.getQuoteFlickerBufferPercent(), askStratConfig.getQuotePicardMaxTicksThrough(),
                        askStratConfig.getPicardMaxPerSec(), askStratConfig.getPicardMaxPerMin(), askStratConfig.getPicardMaxPerHour(),
                        askStratConfig.getPicardMaxPerDay());

                configClient.batchComplete();
            }
        }
    }

    @FromWebSocketView
    public void adoptChild(final String family, final String child, final WebSocketInboundData data) {

        final String familyName;
        if (!families.containsKey(family)) {

            final SearchResult searchResult = searchResults.get(family);
            familyName = getFamilyName(searchResult);
            if (!families.containsKey(familyName)) {
                throw new IllegalArgumentException("Parent [" + family + "] is not known. Should it be created first?");
            }
        } else {
            familyName = family;
        }

        if (!childrenToFamily.containsKey(child)) {

            throw new IllegalArgumentException("Child [" + child + "] is not known. Has it been created in nibbler?");
        } else {

            communityManager.setRelationship(SOURCE_UI, familyName, child);
        }
    }

    @FromWebSocketView
    public void setRelationship(final String childSymbol, final String bidPriceOffsetStr, final String bidQtyMultiplierText,
            final String askPriceOffsetStr, final String askQtyMultiplierText, final String familyToChildRatioText,
            final WebSocketInboundData data) {

        try {
            final double bidPriceOffset = Double.parseDouble(bidPriceOffsetStr);
            final double askPriceOffset = Double.parseDouble(askPriceOffsetStr);
            communityManager.setChildPriceOffsets(SOURCE_UI, childSymbol, bidPriceOffset, askPriceOffset);

            final double bidQtyMultiplier = Double.parseDouble(bidQtyMultiplierText);
            final double askQtyMultiplier = Double.parseDouble(askQtyMultiplierText);
            communityManager.setChildQtyMultipliers(SOURCE_UI, childSymbol, bidQtyMultiplier, askQtyMultiplier);

            final int familyToChildRatio = Integer.parseInt(familyToChildRatioText);
            communityManager.setFamilyToChildRatio(SOURCE_UI, childSymbol, familyToChildRatio);
        } catch (final Exception e) {
            final IStackFamilyUI ui = views.get(data.getOutboundChannel());
            ui.displayErrorMsg(e.getMessage());
        }
    }

    @FromWebSocketView
    public void orphanChild(final String childSymbol, final WebSocketInboundData data) {

        communityManager.orphanChild(SOURCE_UI, childSymbol);
    }

    @FromWebSocketView
    public void killChild(final String childSymbol, final WebSocketInboundData data) {

        killStrategy(childSymbol);
    }

    @FromWebSocketView
    public void killExpiredFutures(final WebSocketInboundData data) {

        final Calendar cal = DateTimeUtil.getCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        DateTimeUtil.setToMidnight(cal);

        final long midnight = cal.getTimeInMillis();

        for (final StackUIData stackData : childData.values()) {

            final FutureConstant future = FutureConstant.getFutureFromSymbol(stackData.symbol);
            if (null != future) {

                expiryCalc.setToRollDate(cal, stackData.symbol);

                if (cal.getTimeInMillis() < midnight) {
                    killStrategy(stackData.symbol);
                }
            }
        }
    }

    private void killStrategy(final String childSymbol) {

        if (childrenToFamily.containsKey(childSymbol)) {
            communityManager.orphanChild(SOURCE_UI, childSymbol);
        }

        final StackUIData stackData = childData.get(childSymbol);
        if (null != stackData) {

            final StackClientHandler stackClient = nibblerClients.get(stackData.source);
            if (null != stackClient) {
                stackClient.killStrategy(childSymbol);
                stackClient.batchComplete();
            }
        }
    }

    @FromWebSocketView
    public void refreshParent(final String parentSymbol, final WebSocketInboundData data) {

        communityManager.reestablishParentalRule(parentSymbol);
    }

    @FromWebSocketView
    public void increaseOffset(final String familyName, final String bookSide, final int multiplier, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        communityManager.increaseOffset(SOURCE_UI, familyName, side, multiplier);
    }

    @FromWebSocketView
    public void setFilteredSelectedConfig(final String filters, final String configType, final WebSocketInboundData data) {

        final StackConfigType stackConfigType = StackConfigType.valueOf(configType);

        final Collection<String> affectedChildren = getFilteredSymbols(filters);
        for (final String childSymbol : affectedChildren) {

            final String family = childrenToFamily.get(childSymbol);
            communityManager.setChildSelectedConfig(SOURCE_UI, family, childSymbol, stackConfigType);
        }
    }

    @FromWebSocketView
    public void selectConfig(final String familyName, final String configType) {

        final StackConfigType stackConfigType = StackConfigType.valueOf(configType);
        communityManager.setParentSelectedConfig(SOURCE_UI, familyName, stackConfigType);
    }

    @FromWebSocketView
    public void setChildSelectedConfig(final String familyName, final String childSymbol, final String configType,
            final WebSocketInboundData data) {

        final StackConfigType stackConfigType = StackConfigType.valueOf(configType);
        communityManager.setChildSelectedConfig(SOURCE_UI, familyName, childSymbol, stackConfigType);
    }

    @FromWebSocketView
    public void setStackEnabled(final String familyName, final String bookSide, final String stack, final boolean isEnabled,
            final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        final StackType stackType = StackType.valueOf(stack);
        communityManager.setStackEnabled(SOURCE_UI, familyName, side, stackType, isEnabled);
    }

    @FromWebSocketView
    public void setAllStacksEnabled(final String familyName, final boolean isEnabled, final WebSocketInboundData data) {

        for (final BookSide side : BookSide.values()) {
            for (final StackType stackType : StackType.values()) {
                communityManager.setStackEnabled(SOURCE_UI, familyName, side, stackType, isEnabled);
            }
        }
    }

    @FromWebSocketView
    public void setFilteredAllStacksEnabled(final String filters, final boolean isEnabled, final WebSocketInboundData data) {

        final Collection<String> affectedChildren = getFilteredSymbols(filters);

        for (final String childSymbol : affectedChildren) {
            for (final BookSide side : BookSide.values()) {
                for (final StackType stackType : StackType.values()) {

                    final String familyName = childrenToFamily.get(childSymbol);
                    setChildStackEnabled(familyName, childSymbol, side, stackType, isEnabled);
                }
            }
        }
    }

    @FromWebSocketView
    public void setFilteredStackEnabled(final String filters, final String bookSide, final String stack, final boolean isEnabled,
            final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        final StackType stackType = StackType.valueOf(stack);

        final Collection<String> affectedChildren = getFilteredSymbols(filters);
        for (final String childSymbol : affectedChildren) {

            final String familyName = childrenToFamily.get(childSymbol);
            setChildStackEnabled(familyName, childSymbol, side, stackType, isEnabled);
        }
    }

    @FromWebSocketView
    public void setChildStackEnabled(final String familyName, final String childSymbol, final String bookSide, final String stack,
            final boolean isEnabled, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        final StackType stackType = StackType.valueOf(stack);
        setChildStackEnabled(familyName, childSymbol, side, stackType, isEnabled);
    }

    private void setChildStackEnabled(final String familyName, final String childSymbol, final BookSide side, final StackType stackType,
            final boolean isEnabled) {

        final StackUIData childUIData = childData.get(childSymbol);
        if (null != childUIData) {
            try {
                communityManager.setChildStackEnabled(SOURCE_UI, familyName, childSymbol, side, stackType, isEnabled);
            } catch (final NullPointerException ignored) {
                // ignored
            }
        }
    }

    @FromWebSocketView
    public void cleanParent(final String familyName, final WebSocketInboundData data) {
        communityManager.cleanParentStack(SOURCE_UI, familyName);
    }

    @FromWebSocketView
    public void startAll(final WebSocketInboundData data) {
        communityManager.startFamilies(families.keySet(), BookSide.BID);
        communityManager.startFamilies(families.keySet(), BookSide.ASK);
    }

    @FromWebSocketView
    public void stopAll(final WebSocketInboundData data) {
        communityManager.stopFamilies(families.keySet(), BookSide.BID);
        communityManager.stopFamilies(families.keySet(), BookSide.ASK);
    }

    @FromWebSocketView
    public void startFiltered(final String filters, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);

        final Collection<String> affectedChildren = getFilteredSymbols(filters);
        for (final String childSymbol : affectedChildren) {

            final String family = childrenToFamily.get(childSymbol);
            communityManager.startChild(family, childSymbol, side);
        }
    }

    @FromWebSocketView
    public void stopFiltered(final String filters, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);

        final Collection<String> affectedChildren = getFilteredSymbols(filters);
        for (final String childSymbol : affectedChildren) {

            final String family = childrenToFamily.get(childSymbol);
            communityManager.stopChild(family, childSymbol, side);
        }
    }

    @FromWebSocketView
    public void startFamily(final String family, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        communityManager.startFamilies(Collections.singleton(family), side);
    }

    @FromWebSocketView
    public void startChild(final String family, final String childSymbol, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        communityManager.startChild(family, childSymbol, side);
    }

    @FromWebSocketView
    public void stopFamily(final String family, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        communityManager.stopFamilies(Collections.singleton(family), side);
    }

    @FromWebSocketView
    public void stopChild(final String family, final String childSymbol, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        communityManager.stopChild(family, childSymbol, side);
    }
}
