package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.manager.persistence.StackPersistenceReader;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.manager.relations.StackOrphanage;
import com.drwtrading.london.eeif.stack.transport.data.config.StackAdditiveConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.config.StackFXConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackLeanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackPlanConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackQuoteConfig;
import com.drwtrading.london.eeif.stack.transport.data.config.StackStrategyConfig;
import com.drwtrading.london.eeif.stack.transport.data.symbology.StackTradableSymbol;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.application.User;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.MDSource;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.ExpiryMonthCodes;
import com.drwtrading.london.eeif.utils.staticData.ExpiryPeriod;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.FutureExpiryCalc;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
import com.drwtrading.london.reddal.stacks.opxl.OpxlStrategySymbolUI;
import com.drwtrading.london.reddal.stacks.strategiesUI.StackStrategiesPresenter;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.workingOrders.obligations.quoting.QuoteObligationsEnableCmd;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

public class StackFamilyView {

    private static final Collection<String> ALLOWED_INST_TYPES = StackStrategiesPresenter.ALLOWED_INST_TYPES;
    private static final StackType[] STACK_TYPES = StackType.values();

    private static final String SOURCE_UI = "FAMILY_ADMIN_UI";

    private static final String MD_SOURCE_FILTER_GROUP = "Trading Venue";
    private static final Pattern FILTER_SPLITTER = Pattern.compile("\\|");

    private static final String EXPIRY_FILTER_GROUP = "Expiries";
    private static final String EXPIRY_FRONT_MONTH_FILTER = "Front months";
    private static final String EXPIRY_BACK_MONTH_FILTER = "Back months";

    private static final String RFQ_SUFFIX = " RFQ";

    private static final double GLOBAL_OFFSET_INCREMENT_BPS = 1d;

    private final SelectIO managementSelectIO;
    private final SelectIO backgroundSelectIO;

    private final StackCommunity community;
    private final SpreadContractSetGenerator contractSetGenerator;
    private final boolean isPrimaryView;

    private final OpxlStrategySymbolUI strategySymbolUI;

    private final Publisher<QuoteObligationsEnableCmd> quotingObligationsCmds;

    private final WebSocketViews<IStackFamilyUI> views;
    private final Map<String, HashSet<IStackFamilyUI>> userViews;

    private final Calendar expiryCal;
    private final FutureExpiryCalc expiryCalc;

    private final Map<String, String> tradableSymbols;

    private final Map<String, FamilyUIData> familyUIData;
    private final Map<String, ChildUIData> childrenUIData;

    private final Map<String, SearchResult> searchResults;
    private final Map<String, LinkedHashSet<String>> fungibleInsts;

    private final Map<String, StackClientHandler> nibblerClients;
    private final Map<String, StackConfigGroup> stackConfigs;

    private final Map<String, String> filterGroups;
    private final Map<String, StackChildFilter> filters;

    private final DecimalFormat priceOffsetDF;

    private StackCommunityManager communityManager;

    private double globalPriceOffsetBPS;

    StackFamilyView(final SelectIO managementSelectIO, final SelectIO backgroundSelectIO, final StackCommunity community,
            final SpreadContractSetGenerator contractSetGenerator, final boolean isSecondaryView,
            final OpxlStrategySymbolUI strategySymbolUI, final Publisher<QuoteObligationsEnableCmd> quotingObligationsCmds) {

        this.managementSelectIO = managementSelectIO;
        this.backgroundSelectIO = backgroundSelectIO;
        this.community = community;

        this.contractSetGenerator = contractSetGenerator;
        this.isPrimaryView = !isSecondaryView;

        this.strategySymbolUI = strategySymbolUI;

        this.quotingObligationsCmds = quotingObligationsCmds;

        this.userViews = new HashMap<>();

        this.expiryCal = DateTimeUtil.getCalendar();
        this.expiryCalc = new FutureExpiryCalc(0);

        this.tradableSymbols = new HashMap<>();

        this.familyUIData = new HashMap<>();
        this.childrenUIData = new HashMap<>();

        this.searchResults = new HashMap<>();
        this.fungibleInsts = new HashMap<>();

        this.nibblerClients = new TreeMap<>();
        this.stackConfigs = new HashMap<>();

        this.filterGroups = new HashMap<>();
        this.filters = new HashMap<>();

        this.priceOffsetDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 1, 3);

        this.globalPriceOffsetBPS = 0d;

        this.views = WebSocketViews.create(IStackFamilyUI.class, this);
    }

    private boolean isFamilyDisplayable(final String familyName) {

        final FamilyUIData parentUIData = familyUIData.get(familyName);
        return null != parentUIData && isFamilyDisplayable(parentUIData);
    }

    private boolean isFamilyDisplayable(final FamilyUIData parentUIData) {
        return community == parentUIData.getStackCommunity();
    }

    private boolean isFamilyAsylum(final String familyName) {

        final FamilyUIData parentUIData = familyUIData.get(familyName);
        return null != parentUIData && isFamilyAsylum(parentUIData);
    }

    private static boolean isFamilyAsylum(final FamilyUIData parentUIData) {
        return StackCommunityManager.ASYLUM_ISIN.equals(parentUIData.uiData.instID.isin);
    }

    void setCommunityManager(final StackCommunityManager communityManager) {
        this.communityManager = communityManager;
    }

    void setStrategyClient(final String nibblerName, final StackClientHandler cache) {
        this.nibblerClients.put(nibblerName, cache);
    }

    void setFilter(final Collection<StackChildFilter> newFilters) {

        for (final StackChildFilter newFilter : newFilters) {

            filters.put(newFilter.filterName, newFilter);
            filterGroups.put(newFilter.filterName, newFilter.groupName);
        }

        views.all().setFilters(filterGroups);
    }

    void addTradableSymbol(final String nibblerName, final StackTradableSymbol tradableSymbol) {

        tradableSymbols.put(tradableSymbol.symbol, nibblerName);
    }

    void setSearchResult(final SearchResult searchResult) {

        if (!searchResult.symbol.endsWith(RFQ_SUFFIX)) {

            searchResults.put(searchResult.symbol, searchResult);

            final Set<String> children = MapUtils.getMappedLinkedSet(fungibleInsts, searchResult.instID.isin);
            children.add(searchResult.symbol);

            updateSymbolFilters(searchResult.symbol);
        }
    }

    void addChildUIData(final StackUIData uiData) {

        final StackFamilyChildRow childRow = new StackFamilyChildRow(uiData);
        updateChildrenUIDataWith(uiData.symbol, childRow);
        updateChildUIData(views.all(), childRow);

        updateSymbolFilters(uiData.symbol);
    }

    void updateChildrenUIDataWith(final String symbol, final StackFamilyChildRow childRow) {

        final ChildUIData childUIData = childrenUIData.get(symbol);
        if (null != childUIData) {
            childUIData.setChildRow(childRow);
        } else {
            childrenUIData.put(symbol, new ChildUIData(symbol, childRow));
        }
    }

    void updateChildUIData(final StackUIData uiData) {

        final StackFamilyChildRow childRow = childrenUIData.get(uiData.symbol).getChildRow();
        if (null != childRow && childRow.updateSnapshot(uiData)) {
            updateChildUIData(views.all(), childRow);
        }
    }

    private void updateChildUIData(final IStackFamilyUI view, final StackFamilyChildRow childRow) {

        final ChildUIData uiData = childrenUIData.get(childRow.getSymbol());

        if (null != uiData && isFamilyDisplayable(uiData.getFamily())) {

            childRow.sendRowState(view);

            switch (childRow.getLeanInstType()) {
                case DR:
                case ETF:
                case EQUITY:
                case FUTURE:
                case FUTURE_SPREAD:
                case FX: {
                    // TODO: THIS instType NEEDS CHANGING
                    strategySymbolUI.addStrategySymbol(community.instType, childRow.getLeanSymbol());
                }
            }
        }
    }

    private void updateSymbolFilters(final String symbol) {

        final SearchResult searchResult = searchResults.get(symbol);
        if (null != searchResult && childrenUIData.containsKey(symbol)) {

            final String filterName = searchResult.mdSource.name();
            final StackChildFilter filter = getFilter(filterName.trim(), MD_SOURCE_FILTER_GROUP);
            filter.addSymbol(symbol);

            if (InstType.FUTURE == community.instType && InstType.FUTURE == searchResult.instType) {

                final FutureConstant future = FutureConstant.getFutureFromSymbol(symbol);

                if (null == future) {
                    System.out.println("Future unknown [" + symbol + "].");
                } else {
                    if (symbol.equals(expiryCalc.getFutureCode(future))) {

                        final StackChildFilter frontMonths = getFilter(EXPIRY_FRONT_MONTH_FILTER, EXPIRY_FILTER_GROUP);
                        frontMonths.addSymbol(symbol);

                    } else if (symbol.equals(expiryCalc.getFutureCode(future, 1))) {

                        final StackChildFilter backMonths = getFilter(EXPIRY_BACK_MONTH_FILTER, EXPIRY_FILTER_GROUP);
                        backMonths.addSymbol(symbol);
                    }

                    expiryCalc.setToRollDate(expiryCal, symbol);
                    final int calendarMonthCode = expiryCal.get(Calendar.MONTH);
                    final ExpiryMonthCodes monthCode = ExpiryMonthCodes.getCode(calendarMonthCode);

                    final StackChildFilter monthCodesFilter = getFilter(monthCode.name(), EXPIRY_FILTER_GROUP);
                    monthCodesFilter.addSymbol(symbol);
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

        final Set<String> result = new HashSet<>();
        for (final Map.Entry<String, ChildUIData> childToParent : childrenUIData.entrySet()) {
            if (isFamilyDisplayable(childToParent.getValue().getFamily())) {
                result.add(childToParent.getKey());
            }
        }
        result.removeAll(familyUIData.keySet());
        result.retainAll(childrenUIData.keySet());

        for (final Set<String> filterGroup : unionSets.values()) {
            result.retainAll(filterGroup);
        }

        return result;
    }

    void addFamilyUIData(final FamilyUIData familyData) {

        final String familyName = familyData.uiData.symbol;

        familyUIData.put(familyName, familyData);

        if (isFamilyDisplayable(familyData)) {

            final boolean isAsylum = isFamilyAsylum(familyData);
            views.all().addFamily(familyName, isAsylum);
            updateFamilyUIData(views.all(), familyData);
        }
    }

    void updateFamilyUIData(final FamilyUIData uiData) {

        updateFamilyUIData(views.all(), uiData);
    }

    private void updateFamilyUIData(final IStackFamilyUI view, final FamilyUIData familyData) {

        final StackUIData uiData = familyData.uiData;

        if (isFamilyDisplayable(uiData.symbol)) {
            view.setParentData(uiData.symbol, uiData.getActiveBidPriceOffsetBPS(), uiData.getActiveAskPriceOffsetBPS(),
                    uiData.isStackEnabled(BookSide.BID, StackType.PICARD), uiData.isStackEnabled(BookSide.BID, StackType.QUOTER),
                    uiData.isStackEnabled(BookSide.ASK, StackType.PICARD), uiData.isStackEnabled(BookSide.ASK, StackType.QUOTER));
        }
    }

    void setConfig(final StackConfigGroup stackConfig) {

        this.stackConfigs.put(stackConfig.getSymbol(), stackConfig);
    }

    public boolean updateRelationship(final String childSymbol, final String parentSymbol, final double bidPriceOffset,
            final double bidQtyMultiplier, final double askPriceOffset, final double askQtyMultiplier, final int familyToChildRatio) {

        for (final Map.Entry<String, FamilyUIData> familyRelations : familyUIData.entrySet()) {

            final FamilyUIData familyUIData = familyRelations.getValue();
            if (familyUIData.removeChild(childSymbol) && !isFamilyDisplayable(parentSymbol)) {
                final String oldFamily = familyRelations.getKey();
                views.all().removeChild(oldFamily, childSymbol);
            }
        }

        final StackUIRelationship newRelationship =
                new StackUIRelationship(childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier,
                        familyToChildRatio);

        final FamilyUIData familyUIData = this.familyUIData.get(parentSymbol);
        familyUIData.addChild(childSymbol, newRelationship);

        setChildUIDataField(childSymbol, parentSymbol);

        if (isFamilyDisplayable(parentSymbol)) {

            strategySymbolUI.addStrategySymbol(community.instType, childSymbol);

            views.all().setChild(parentSymbol, childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier,
                    familyToChildRatio);

            if (!parentSymbol.equals(StackOrphanage.ORPHANAGE) && !isFamilyAsylum(parentSymbol)) {
                contractSetGenerator.setParentStack(childSymbol, parentSymbol);
            }
        }
        return true;
    }

    private void setChildUIDataField(final String childSymbol, final String parentSymbol) {

        final ChildUIData childUIData = childrenUIData.get(childSymbol);
        if (null != childUIData) {
            childUIData.setFamily(parentSymbol);
        } else {
            childrenUIData.put(childSymbol, new ChildUIData(childSymbol, parentSymbol));
        }
    }

    public void symbolSelected(final SymbolSelection symbolSelection) {

        final Set<IStackFamilyUI> views = userViews.get(symbolSelection.username);
        if (null != views) {
            for (final IStackFamilyUI view : views) {

                view.showChild(symbolSelection.symbol);
            }
        }
    }

    void addUI(final String username, final Publisher<WebSocketOutboundData> channel) {

        final IStackFamilyUI newView = views.get(channel);
        MapUtils.getMappedSet(userViews, username).add(newView);

        newView.setFilters(filterGroups);

        for (final Map.Entry<String, FamilyUIData> family : familyUIData.entrySet()) {

            final String familyName = family.getKey();
            if (isFamilyDisplayable(familyName)) {

                final FamilyUIData parentUIData = familyUIData.get(familyName);
                final boolean isAsylum = isFamilyAsylum(parentUIData);
                newView.addFamily(familyName, isAsylum);

                for (final StackUIRelationship child : family.getValue().getAllRelationships()) {

                    newView.setChild(familyName, child.childSymbol, child.bidPriceOffsetBPS, child.bidQtyMultiplier,
                            child.askPriceOffsetBPS, child.askQtyMultiplier, child.familyToChildRatio);
                }
            }
        }

        for (final FamilyUIData familyUIData : familyUIData.values()) {
            updateFamilyUIData(newView, familyUIData);
        }

        for (final ChildUIData uiData : childrenUIData.values()) {
            if (null != uiData.getChildRow()) {
                updateChildUIData(newView, uiData.getChildRow());
            }
        }

        presentGlobalOffset(newView);
        presentGlobalStackEnabling(newView);
    }

    void handleWebMsg(final WebSocketInboundData msg) {
        views.invoke(msg);
    }

    void removeUI(final String username, final WebSocketDisconnected disconnected) {

        final IStackFamilyUI oldView = views.unregister(disconnected);
        userViews.get(username).remove(oldView);
    }

    @FromWebSocketView
    public void refreshAllParents(final WebSocketInboundData data) {
        communityManager.reestablishParentalRule(community);
    }

    @FromWebSocketView
    public void cleanAllParents(final WebSocketInboundData data) {

        if (InstType.FUTURE == community.instType) {
            cleanFuturesAroundFrontMonth();
        } else {
            communityManager.cleanParentStacks(SOURCE_UI, community);
        }
    }

    private void cleanFuturesAroundFrontMonth() {

        for (final FutureConstant future : FutureConstant.values()) {

            final String frontMonthSymbol = expiryCalc.getFutureCode(future, 0);
            final ChildUIData childUIData = childrenUIData.get(frontMonthSymbol);

            if (null != childUIData && null != childUIData.getFamily()) {
                final String familyName = childUIData.getFamily();
                communityManager.cleanParentStackAroundChild(SOURCE_UI, familyName, frontMonthSymbol);
            }
        }
    }

    @FromWebSocketView
    public void increaseGlobalPriceOffset(final int multiplier, final WebSocketInboundData data) {

        globalPriceOffsetBPS += multiplier * GLOBAL_OFFSET_INCREMENT_BPS;
        updateCommunityManagerGlobalOffset();
    }

    @FromWebSocketView
    public void decreaseGlobalPriceOffset(final int multiplier, final WebSocketInboundData data) {

        globalPriceOffsetBPS -= multiplier * GLOBAL_OFFSET_INCREMENT_BPS;
        globalPriceOffsetBPS = Math.max(globalPriceOffsetBPS, 0d);
        updateCommunityManagerGlobalOffset();
    }

    private void updateCommunityManagerGlobalOffset() {

        final double sideOffsetBPS = globalPriceOffsetBPS / 2;
        communityManager.setCommunityOffsets(community, -sideOffsetBPS, sideOffsetBPS);

        presentGlobalOffset(views.all());
    }

    private void presentGlobalOffset(final IStackFamilyUI view) {
        final String prettyGlobalOffsetBPS = priceOffsetDF.format(globalPriceOffsetBPS);
        view.setGlobalOffset(prettyGlobalOffsetBPS);
    }

    @FromWebSocketView
    public void globalStackEnabled(final String bookSide, final String stack, final boolean isEnabled, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        final StackType stackType = StackType.valueOf(stack);
        communityManager.setStackEnabled(community, side, stackType, isEnabled);

        presentGlobalStackEnabling(views.all());
    }

    private void presentGlobalStackEnabling(final IStackFamilyUI view) {

        final boolean isBidPicardEnabled = communityManager.getStackState(community, BookSide.BID, StackType.PICARD);
        final boolean isBidQuoteEnabled = communityManager.getStackState(community, BookSide.BID, StackType.QUOTER);
        final boolean isAskQuoteEnabled = communityManager.getStackState(community, BookSide.ASK, StackType.QUOTER);
        final boolean isAskPicardEnabled = communityManager.getStackState(community, BookSide.ASK, StackType.PICARD);

        view.setGlobalStackEnabled(isBidPicardEnabled, isBidQuoteEnabled, isAskQuoteEnabled, isAskPicardEnabled);
    }

    @FromWebSocketView
    public void saveOffsets() {
        communityManager.saveOffsets();
        views.all().offsetsSaved();
    }

    @FromWebSocketView
    public void loadOffsets() {

        backgroundSelectIO.execute(() -> {
            try {
                final StackPersistenceReader loadedOffsets = communityManager.readSavedOffsets();
                managementSelectIO.execute(() -> communityManager.loadOffsets(loadedOffsets));
            } catch (final Exception e) {
                managementSelectIO.execute(() -> views.all().displayErrorMsg(e.getMessage()));
            }
            views.all().offsetsLoaded();
        });
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
    public void findFamilyMembers(final String symbol, final boolean isADRName, final WebSocketInboundData data) {

        final SearchResult searchResult = searchResults.get(symbol);
        if (null != searchResult) {

            final IStackFamilyUI ui = views.get(data.getOutboundChannel());
            final String family = getFamilyName(searchResult, isADRName);
            final boolean isFamilyExists = familyUIData.containsKey(family);
            ui.setCreateFamilyRow(symbol, isFamilyExists, family);

            if (InstType.FUTURE == searchResult.instType) {

                final FutureConstant future = FutureConstant.getFutureFromSymbol(symbol);
                if (null != future) {
                    for (int i = 0; i < 3; ++i) {

                        final String expirySymbol = expiryCalc.getFutureCode(future, i);
                        final SearchResult expirySearchResult = searchResults.get(expirySymbol);

                        if (null != expirySearchResult) {

                            final boolean isChildAlreadyCreated = this.childrenUIData.containsKey(expirySymbol);
                            final String tradableNibbler = tradableSymbols.get(expirySymbol);
                            ui.addCreateChildRow(expirySymbol, isChildAlreadyCreated, nibblerClients.keySet(), tradableNibbler,
                                    ALLOWED_INST_TYPES, InstType.INDEX.name(), expirySymbol);
                        }
                    }
                }
            } else {
                final Set<String> children = fungibleInsts.get(searchResult.instID.isin);
                for (final String childSymbol : children) {

                    final boolean isChildAlreadyCreated = this.childrenUIData.containsKey(childSymbol);
                    final String tradableNibbler = tradableSymbols.get(childSymbol);

                    if (isPrimaryView) {
                        ui.addCreateChildRow(childSymbol, isChildAlreadyCreated, nibblerClients.keySet(), tradableNibbler,
                                ALLOWED_INST_TYPES, InstType.INDEX.name(), childSymbol);
                    } else if (!symbol.equals(childSymbol) || isADRName) {
                        ui.addCreateChildRow(childSymbol, isChildAlreadyCreated, nibblerClients.keySet(), tradableNibbler,
                                ALLOWED_INST_TYPES, InstType.EQUITY.name(), symbol);
                    }
                }
            }
        }
    }

    @FromWebSocketView
    public void checkFamilyExists(final String family, final String resultFieldID, final WebSocketInboundData data) {

        final IStackFamilyUI ui = views.get(data.getOutboundChannel());

        if (familyUIData.containsKey(family)) {

            ui.setFieldData(resultFieldID, "Family found.");
        } else {

            ui.clearFieldData(resultFieldID);
        }
    }

    @FromWebSocketView
    public void createFamily(final String symbol, final boolean isAsylum, final boolean isADRName, final WebSocketInboundData data) {

        if (isAsylum) {

            communityManager.createAsylum(SOURCE_UI, symbol, community.instType, community);
        } else {
            final SearchResult searchResult = searchResults.get(symbol);
            if (null != searchResult) {

                final String family = getFamilyName(searchResult, isADRName);
                final InstrumentID instID = searchResult.instID;

                communityManager.createFamily(SOURCE_UI, family, instID, community.instType, community);
            }
        }
    }

    @FromWebSocketView
    public void killFamily(final String familyName) {
        communityManager.killFamily(SOURCE_UI, familyName);
    }

    @FromWebSocketView
    public void createNamedFamily(final String familyName, final String isin, final String ccyName, final String micName,
            final WebSocketInboundData data) {

        final CCY ccy = CCY.getCCY(ccyName);
        final MIC mic = MIC.getMIC(micName);
        final InstrumentID instID = new InstrumentID(isin, ccy, mic);

        communityManager.createFamily(SOURCE_UI, familyName, instID, community.instType, community);
    }

    private static String getFamilyName(final SearchResult searchResult, final boolean isADR) {

        switch (searchResult.instType) {
            case FUTURE: {
                return FutureConstant.getFutureFromSymbol(searchResult.symbol).name();
            }
            case FUTURE_SPREAD: {
                final String frontMonth = searchResult.symbol.split("-")[0];
                return FutureConstant.getFutureFromSymbol(frontMonth).name();
            }
            default: {
                final String familyBaseName = searchResult.symbol.split(" ")[0];
                if (isADR) {
                    return "ADR" + familyBaseName;
                } else {
                    return familyBaseName;
                }
            }
        }
    }

    @FromWebSocketView
    public void checkChildExists(final String child, final String resultFieldID, final WebSocketInboundData data) {

        final IStackFamilyUI ui = views.get(data.getOutboundChannel());

        if (childrenUIData.containsKey(child)) {

            ui.setFieldData(resultFieldID, "Child available.");
        } else {

            ui.clearFieldData(resultFieldID);
        }
    }

    @FromWebSocketView
    public void createChildStack(final String nibblerName, final String quoteSymbol, final String leanInstrumentType,
            final String leanSymbol, final String additiveSymbol, final WebSocketInboundData data) {

        final StackClientHandler strategyClient = nibblerClients.get(nibblerName);
        if (null != strategyClient) {

            final InstrumentID quoteInstId = searchResults.get(quoteSymbol).instID;

            final InstType leanInstType = InstType.getInstType(leanInstrumentType);
            final InstrumentID leanInstID = searchResults.get(leanSymbol).instID;

            if (null != quoteInstId && null != leanInstType && null != leanInstID) {
                strategyClient.createStrategy(quoteSymbol, quoteInstId, leanInstType, leanSymbol, leanInstID, additiveSymbol);
                strategyClient.batchComplete();
            }
        }
    }

    @FromWebSocketView
    public void createMissingChildren(final WebSocketInboundData data) {

        for (final FamilyUIData familyUIData : familyUIData.values()) {

            final StackUIData stackUIData = familyUIData.uiData;
            final Collection<String> childSymbols = fungibleInsts.get(stackUIData.instID.isin);
            if (null != childSymbols) {
                for (final String childSymbol : childSymbols) {

                    final SearchResult searchResult = searchResults.get(childSymbol);
                    final String tradableNibbler = tradableSymbols.get(childSymbol);
                    final StackClientHandler strategyClient = nibblerClients.get(tradableNibbler);

                    if (null != searchResult && null != strategyClient) {
                        strategyClient.createStrategy(searchResult.symbol, searchResult.instID, InstType.INDEX, searchResult.symbol,
                                searchResult.instID, "");
                        strategyClient.batchComplete();
                    }
                }
            }
        }
    }

    @FromWebSocketView
    public void correctAdoptionsForChildren(final WebSocketInboundData data) {

        for (final Map.Entry<String, ChildUIData> childFamily : childrenUIData.entrySet()) {

            final String childSymbol = childFamily.getKey();
            final String familyName = childFamily.getValue().getFamily();

            final SearchResult searchResult = searchResults.get(childSymbol);
            if (null != searchResult) {

                final String properFamilyName = getExistingFamily(searchResult.instID.isin);

                if (null != properFamilyName && !properFamilyName.equals(familyName)) {
                    communityManager.setRelationship(SOURCE_UI, properFamilyName, childSymbol);
                }
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
                final StackFamilyChildRow frontMonthData = childrenUIData.get(frontMonthSymbol).getChildRow();

                for (int i = 1; i < 3; ++i) {

                    final String backMonthSymbol = expiryCalc.getFutureCode(future, i);
                    final SearchResult backMonthSearchResult = searchResults.get(backMonthSymbol);
                    final StackFamilyChildRow backMonthData = childrenUIData.get(backMonthSymbol).getChildRow();

                    if (null != frontMonthData && null == backMonthData && null != backMonthSearchResult) {

                        final StackClientHandler strategyClient = nibblerClients.get(frontMonthData.getSource());

                        if (null != strategyClient) {

                            final String additiveSymbol;
                            if (frontMonthData.getAdditiveSymbol().equals(frontMonthData.getSymbol())) {
                                additiveSymbol = backMonthSymbol;
                            } else {
                                additiveSymbol = frontMonthData.getAdditiveSymbol();
                            }

                            strategyClient.createStrategy(backMonthSymbol, backMonthSearchResult.instID, InstType.INDEX, backMonthSymbol,
                                    backMonthSearchResult.instID, additiveSymbol);
                            strategyClient.batchComplete();
                        }
                    }
                }
            }
        }
    }

    private void rollFutures(final ExpiryPeriod expiryPeriod) {

        for (final FutureConstant future : FutureConstant.values()) {

            if (expiryPeriod == future.expiryPeriod) {

                final String fromSymbol = expiryCalc.getFutureCode(future, 0);

                for (int i = 1; i < 3; ++i) {

                    final String toSymbol = expiryCalc.getFutureCode(future, i);
                    copyChildSetup(fromSymbol, toSymbol, null);
                }
            }
        }
    }

    @FromWebSocketView
    public void copyChildSetup(final String fromSymbol, final String toSymbol, final WebSocketInboundData data) {

        final String family = childrenUIData.get(toSymbol).getFamily();
        communityManager.stopChild(family, toSymbol, BookSide.BID);
        communityManager.stopChild(family, toSymbol, BookSide.ASK);

        final StackConfigGroup fromConfig = stackConfigs.get(fromSymbol);
        final StackConfigGroup toConfig = stackConfigs.get(toSymbol);
        final StackFamilyChildRow toChildUIData = childrenUIData.get(fromSymbol).getChildRow();

        if (null != fromConfig && null != toConfig && null != toChildUIData) {

            communityManager.copyChildStacks(SOURCE_UI, fromSymbol, toSymbol);
            final StackClientHandler configClient = nibblerClients.get(toChildUIData.getSource());

            final StackQuoteConfig quoteConfig = fromConfig.quoteConfig;
            configClient.quoteConfigUpdated(SOURCE_UI, toConfig.configGroupID, quoteConfig.getMaxBookAgeMillis(),
                    quoteConfig.isAuctionQuotingEnabled(), quoteConfig.isOnlyAuctionQuoting(), quoteConfig.getAuctionTheoMaxBPSThrough(),
                    quoteConfig.isAllowEmptyBook(), quoteConfig.getMaxJumpBPS(), quoteConfig.getBettermentQty(),
                    quoteConfig.getBettermentTicks(), quoteConfig.isBettermentOppositeSide(), quoteConfig.getOppositeSideBettermentTicks());

            final StackFXConfig fxConfig = fromConfig.fxConfig;
            configClient.fxConfigUpdated(SOURCE_UI, toConfig.configGroupID, fxConfig.getMaxBookAgeMillis(), fxConfig.getMaxJumpBPS());

            final StackLeanConfig leanConfig = fromConfig.leanConfig;
            configClient.leanConfigUpdated(SOURCE_UI, toConfig.configGroupID, leanConfig.getMaxBookAgeMillis(), leanConfig.getMaxJumpBPS(),
                    leanConfig.getRequiredQty(), leanConfig.getMaxPapaWeight(), leanConfig.getLeanToQuoteRatio(),
                    leanConfig.getPriceAdjustment());

            final StackAdditiveConfig additiveConfig = fromConfig.additiveConfig;
            configClient.additiveConfigUpdated(SOURCE_UI, toConfig.configGroupID, additiveConfig.getMaxSignalAgeMillis(),
                    additiveConfig.isEnabled(), additiveConfig.getMinRequiredBPS(), additiveConfig.getMaxBPS());

            final StackPlanConfig planConfig = fromConfig.planConfig;
            configClient.planConfigUpdated(SOURCE_UI, toConfig.configGroupID, planConfig.getMinLevelQty(), planConfig.getMaxLevelQty(),
                    planConfig.getLotSize(), planConfig.getMaxLevels(), planConfig.getMinPicardQty());

            final StackStrategyConfig stratConfig = fromConfig.strategyConfig;
            configClient.strategyConfigUpdated(SOURCE_UI, toConfig.configGroupID, stratConfig.getMaxOrdersPerLevel(),
                    stratConfig.isOnlySubmitBestLevel(), stratConfig.isQuoteBettermentOn(), stratConfig.getModTicks(),
                    stratConfig.getQuoteFlickerBufferPercent(), stratConfig.getQuotePicardMaxBPSThrough(),
                    stratConfig.getPicardMaxPapaWeight(), stratConfig.getPicardMaxPerSec(), stratConfig.getPicardMaxPerMin(),
                    stratConfig.getPicardMaxPerHour(), stratConfig.getPicardMaxPerDay());

            configClient.batchComplete();
        }
    }

    @FromWebSocketView
    public void adoptChild(final String family, final String child, final boolean isADRName, final WebSocketInboundData data) {

        final String familyName;
        if (familyUIData.containsKey(family)) {
            familyName = family;
        } else {

            final SearchResult searchResult = searchResults.get(family);
            familyName = getFamilyName(searchResult, isADRName);
            if (!familyUIData.containsKey(familyName)) {
                throw new IllegalArgumentException("Parent [" + family + "] is not known. Should it be created first?");
            }
        }

        if (!childrenUIData.containsKey(child)) {

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
    public void resetFamilyOffsets(final WebSocketInboundData data) {

        try {

            for (final Map.Entry<String, FamilyUIData> family : familyUIData.entrySet()) {

                final FamilyUIData familyData = family.getValue();

                if (null != familyData && InstType.ETF == familyData.uiData.leanInstType) {

                    final Set<StackUIRelationship> childSymbols = new HashSet<>(familyData.getAllRelationships());

                    for (final StackUIRelationship child : childSymbols) {

                        final String childSymbol = child.childSymbol;

                        if (childrenUIData.containsKey(childSymbol)) {
                            final double offset = ChildOffsetCalculator.getSymbolOffset(childSymbol);
                            communityManager.setChildPriceOffsets(SOURCE_UI, childSymbol, -offset, offset);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            final IStackFamilyUI ui = views.get(data.getOutboundChannel());
            ui.displayErrorMsg(e.getMessage());
            e.printStackTrace();
        }
    }

    @FromWebSocketView
    public void resetOffsetsForFamily(final String familySymbol, final boolean isADRName, final WebSocketInboundData data) {
        try {

            final SearchResult searchResult = searchResults.get(familySymbol);
            if (null == searchResult) {
                return;
            }

            final String familyName = getFamilyName(searchResult, isADRName);
            final FamilyUIData family = familyUIData.get(familyName);

            if (null == family) {
                System.out.println("Couldn't find [" + familyName + "] for symbol [" + familySymbol + "].");
            } else {

                final StackUIData parentUIData = family.uiData;
                if (null != parentUIData && InstType.ETF == parentUIData.leanInstType) {

                    final Set<StackUIRelationship> children = new HashSet<>(family.getAllRelationships());
                    for (final StackUIRelationship child : children) {

                        final String childSymbol = child.childSymbol;
                        if (childrenUIData.containsKey(childSymbol)) {

                            final double offset = ChildOffsetCalculator.getSymbolOffset(childSymbol);
                            communityManager.setChildPriceOffsets(SOURCE_UI, childSymbol, -offset, offset);
                        }
                    }
                }
            }
        } catch (final Exception e) {
            final IStackFamilyUI ui = views.get(data.getOutboundChannel());
            ui.displayErrorMsg(e.getMessage());
            e.printStackTrace();
        }
    }

    @FromWebSocketView
    public void updateOffsets(final String familySymbol, final Integer bpsWider, final Boolean skipNonDefaults,
            final WebSocketInboundData data) {

        for (final Map.Entry<String, FamilyUIData> family : familyUIData.entrySet()) {
            try {
                final String familyName = family.getKey();

                if (familySymbol.equals(familyName) || "*".equals(familySymbol)) {

                    final FamilyUIData familyData = family.getValue();

                    final StackUIData parentUIData = familyData.uiData;

                    if (null != parentUIData && InstType.ETF == parentUIData.leanInstType) {

                        final Collection<StackUIRelationship> children = new HashSet<>(familyData.getAllRelationships());

                        boolean isFamilyUpdated = false;

                        for (final StackUIRelationship childData : children) {

                            final double offset = ChildOffsetCalculator.getSymbolOffset(childData.childSymbol);
                            final double bidOffset = Math.abs(childData.bidPriceOffsetBPS);
                            final double askOffset = childData.askPriceOffsetBPS;
                            if (Constants.EPSILON < Math.abs(bidOffset - offset) || Constants.EPSILON < (askOffset - offset)) {
                                isFamilyUpdated = true;
                            }
                        }

                        if (skipNonDefaults && isFamilyUpdated) {
                            continue;
                        }

                        for (final StackUIRelationship childData : children) {

                            final double offset = ChildOffsetCalculator.getSymbolOffset(childData.childSymbol);
                            if (offset == 0.0) {
                                continue;
                            }
                            communityManager.setChildPriceOffsets(SOURCE_UI, childData.childSymbol, -offset - bpsWider, offset + bpsWider);
                            final SearchResult searchResult = searchResults.get(childData.childSymbol);
                            if (null != searchResult && searchResult.mdSource == MDSource.RFQ) {
                                communityManager.setChildQtyMultipliers(SOURCE_UI, childData.childSymbol, 10, 10);
                            }
                        }

                    }
                }
            } catch (final Exception e) {
                final IStackFamilyUI ui = views.get(data.getOutboundChannel());
                ui.displayErrorMsg(e.getMessage());
                e.printStackTrace();
            }
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
        killExpiredFutures(0);
    }

    @FromWebSocketView
    public void killExpiringFutures(final WebSocketInboundData data) {
        killExpiredFutures(90);
    }

    private void killExpiredFutures(final int daysAhead) {

        final Calendar cal = DateTimeUtil.getCalendar();
        cal.setTimeInMillis(System.currentTimeMillis());
        cal.add(Calendar.DAY_OF_YEAR, daysAhead);
        DateTimeUtil.setToMidnight(cal);

        final long midnight = cal.getTimeInMillis();

        cal.add(Calendar.YEAR, 9);

        final long nineYearsAhead = cal.getTimeInMillis();

        for (final Map.Entry<String, FamilyUIData> familyChildren : familyUIData.entrySet()) {

            if (!StackOrphanage.ORPHANAGE.equals(familyChildren.getKey())) {

                for (final StackUIRelationship child : familyChildren.getValue().getAllRelationships()) {

                    final String childSymbol = child.childSymbol;

                    final FutureConstant future = FutureConstant.getFutureFromSymbol(childSymbol);
                    if (null != future && FutureConstant.FEXD != future) {

                        expiryCalc.setToRollDate(cal, childSymbol);
                        final long expiryMillis = cal.getTimeInMillis();

                        if (expiryMillis < midnight || nineYearsAhead < expiryMillis) {
                            killStrategy(childSymbol);
                        }
                    }
                }
            }
        }
    }

    private void killStrategy(final String childSymbol) {

        if (childrenUIData.containsKey(childSymbol)) {
            communityManager.orphanChild(SOURCE_UI, childSymbol);
        }

        final ChildUIData childRow = childrenUIData.get(childSymbol);
        if (null != childRow && null != childRow.getChildRow()) {

            final StackClientHandler stackClient = nibblerClients.get(childRow.getChildRow().getSource());
            if (null != stackClient) {
                stackClient.killStrategy(childSymbol);
                stackClient.batchComplete();
            }
        }
    }

    @FromWebSocketView
    public void createAllRFQ(final WebSocketInboundData data) {

        final StackClientHandler strategyClient = nibblerClients.get("rfq");

        for (final SearchResult searchResult : searchResults.values()) {

            if (MDSource.RFQ == searchResult.mdSource) {

                final String rfqSymbol = searchResult.symbol;
                final InstrumentID rfqInstId = searchResult.instID;

                if (null != strategyClient && !childrenUIData.containsKey(rfqSymbol)) {

                    strategyClient.createStrategy(rfqSymbol, rfqInstId, InstType.INDEX, rfqSymbol, rfqInstId, "");
                    strategyClient.batchComplete();
                }
            }
        }
    }

    @FromWebSocketView
    public void adoptAllRFQ() {
        for (final SearchResult searchResult : searchResults.values()) {
            if (MDSource.RFQ == searchResult.mdSource) {
                final String familyName = getExistingFamily(searchResult.instID.isin);
                final String rfqSymbol = searchResult.symbol;
                if (null != familyName && childrenUIData.containsKey(rfqSymbol)) {
                    communityManager.setRelationship(SOURCE_UI, familyName, rfqSymbol);
                    final FamilyUIData familyData = familyUIData.get(familyName);
                    if (null != familyData) {

                        final StackUIRelationship stackUIRelationship = familyData.getChildRelationship(rfqSymbol);

                        if (null != stackUIRelationship && Math.abs(stackUIRelationship.bidQtyMultiplier) < 0.1 &&
                                Math.abs(stackUIRelationship.askQtyMultiplier) < 0.1) {

                            communityManager.setChildQtyMultipliers(SOURCE_UI, rfqSymbol, 10, 10);
                        }
                    }
                }
            }
        }
    }

    private String getExistingFamily(final String isin) {

        final LinkedHashSet<String> symbols = fungibleInsts.get(isin);
        for (final String symbol : symbols) {

            final SearchResult searchResult = searchResults.get(symbol);
            if (isin.equals(searchResult.instID.isin)) {

                final String family = getFamilyName(searchResult, false);
                if (familyUIData.containsKey(family)) {
                    return family;
                } else {
                    final String adrFamily = getFamilyName(searchResult, true);
                    if (familyUIData.containsKey(adrFamily)) {
                        return adrFamily;
                    }
                }
            }
        }
        return null;
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
    public void lookupConfigSymbols(final String filters, final WebSocketInboundData data) {

        final Collection<String> affectedChildren = getFilteredSymbols(filters);
        if (!affectedChildren.isEmpty()) {

            final StringBuilder sb = new StringBuilder();
            for (final String symbol : affectedChildren) {
                sb.append(symbol);
                sb.append(',');
            }

            sb.setLength(sb.length() - 1);
            final IStackFamilyUI ui = views.get(data.getOutboundChannel());
            ui.openConfig(sb.toString());
        }
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

                    final ChildUIData childUIData = childrenUIData.get(childSymbol);
                    setChildStackEnabled(childUIData.getFamily(), childSymbol, side, stackType, isEnabled);
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

            final ChildUIData childUIData = childrenUIData.get(childSymbol);
            setChildStackEnabled(childUIData.getFamily(), childSymbol, side, stackType, isEnabled);
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

        if (childrenUIData.containsKey(childSymbol)) {
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

        final User user = User.get(data.getClient().getUserName());

        for (final FamilyUIData familyUIData : familyUIData.values()) {
            if (isFamilyDisplayable(familyUIData)) {
                communityManager.reestablishParentalRule(familyUIData.uiData.symbol);
            }
        }

        communityManager.startFamilies(community, BookSide.BID, user);
        communityManager.startFamilies(community, BookSide.ASK, user);

        // TODO jcoutinho - remove this once quoting obligations has been split out properly
        if (isPrimaryView && quotingObligationsEnabled()) {
            quotingObligationsCmds.publish(new QuoteObligationsEnableCmd(user));
        }
    }

    private boolean quotingObligationsEnabled() {
        return EnumSet.of(StackCommunity.DM, StackCommunity.FUTURE).contains(community);
    }

    @FromWebSocketView
    public void stopAll(final WebSocketInboundData data) {
        communityManager.stopFamilies(BookSide.BID, community);
        communityManager.stopFamilies(BookSide.ASK, community);
    }

    @FromWebSocketView
    public void startFiltered(final String filters, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        final User user = User.get(data.getClient().getUserName());

        final Collection<String> affectedChildren = getFilteredSymbols(filters);
        for (final String childSymbol : affectedChildren) {

            final ChildUIData childUIData = childrenUIData.get(childSymbol);
            communityManager.startChild(childUIData.getFamily(), childSymbol, side, user);
        }
    }

    @FromWebSocketView
    public void stopFiltered(final String filters, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);

        final Collection<String> affectedChildren = getFilteredSymbols(filters);
        for (final String childSymbol : affectedChildren) {

            final ChildUIData childUIData = childrenUIData.get(childSymbol);
            communityManager.stopChild(childUIData.getFamily(), childSymbol, side);
        }
    }

    @FromWebSocketView
    public void startFamily(final String family, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        final User user = User.get(data.getClient().getUserName());

        communityManager.startFamilies(Collections.singleton(family), side, user);
    }

    @FromWebSocketView
    public void startChild(final String family, final String childSymbol, final String bookSide, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        final User user = User.get(data.getClient().getUserName());

        communityManager.startChild(family, childSymbol, side, user);
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

    // TODO: MOVE THIS TO Community manager
    public void setChildStackEnabled(final String source, final String familyName, final BookSide side, final boolean isEnabled) {

        if (StackCommunity.DM == community || StackCommunity.FUTURE == community) {
            final FamilyUIData children = familyUIData.get(familyName);

            if (null != children) {
                for (final StackUIRelationship child : children.getAllRelationships()) {

                    final String childSymbol = child.childSymbol;

                    if (childrenUIData.containsKey(childSymbol)) {
                        for (final StackType stackType : STACK_TYPES) {
                            communityManager.setChildStackEnabled(source, familyName, childSymbol, side, stackType, isEnabled);
                        }
                    }
                }
            }
        }
    }
}
