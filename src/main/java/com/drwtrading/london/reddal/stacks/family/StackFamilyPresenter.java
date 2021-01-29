package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.jetlang.autosubscribe.TypedChannel;
import com.drwtrading.london.eeif.stack.manager.StackManagerComponents;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.manager.relations.StackOrphanage;
import com.drwtrading.london.eeif.stack.transport.cache.relationships.IStackRelationshipListener;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.symbology.StackTradableSymbol;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.monitoring.IErrorLogger;
import com.drwtrading.london.eeif.utils.monitoring.IFuseBox;
import com.drwtrading.london.eeif.utils.staticData.CCY;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.eeif.utils.staticData.MIC;
import com.drwtrading.london.indy.transport.data.ETFDef;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
import com.drwtrading.london.reddal.stacks.opxl.OpxlStrategySymbolUI;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.obligations.quoting.QuoteObligationsEnableCmd;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Date;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public class StackFamilyPresenter implements IStackRelationshipListener {

    private final UILogger uiLogger;

    private final EnumMap<StackCommunity, StackFamilyView> communityViews;

    private final Map<Publisher<WebSocketOutboundData>, StackFamilyView> userViews;
    private final Set<StackCommunity> primaryCommunities;

    private final Map<String, FamilyUIData> familiesData;

    public StackFamilyPresenter(final SelectIO presenterSelectIO, final SelectIO backgroundSelectIO,
            final IFuseBox<StackManagerComponents> fuseBox, final IErrorLogger errorLogger, final UILogger uiLogger,
            final SpreadContractSetGenerator contractSetGenerator, final Set<StackCommunity> primaryCommunities,
            final Set<StackCommunity> otherCommunities, final OpxlStrategySymbolUI strategySymbolUI,
            final Publisher<QuoteObligationsEnableCmd> quotingObligationsCmds,
            final EnumMap<StackCommunity, TypedChannel<String>> communitySymbols,
            final EnumMap<StackCommunity, TypedChannel<String>> communityIsins) {

        this.uiLogger = uiLogger;
        this.primaryCommunities = primaryCommunities;

        this.communityViews = new EnumMap<>(StackCommunity.class);

        for (final StackCommunity primaryCommunity : primaryCommunities) {
            final StackFamilyView familyView =
                    new StackFamilyView(presenterSelectIO, backgroundSelectIO, fuseBox, errorLogger, primaryCommunity, contractSetGenerator,
                            false, strategySymbolUI, quotingObligationsCmds, communitySymbols.get(primaryCommunity),
                            communityIsins.get(primaryCommunity));
            communityViews.put(primaryCommunity, familyView);
            otherCommunities.remove(primaryCommunity);
        }

        otherCommunities.add(StackCommunity.EXILES);
        otherCommunities.add(StackCommunity.ORPHANAGE);

        for (final StackCommunity stackCommunity : otherCommunities) {

            final StackFamilyView asylumView =
                    new StackFamilyView(presenterSelectIO, backgroundSelectIO, fuseBox, errorLogger, stackCommunity, contractSetGenerator,
                            true, strategySymbolUI, Constants::NO_OP, communitySymbols.get(stackCommunity),
                            communityIsins.get(stackCommunity));
            communityViews.put(stackCommunity, asylumView);
        }

        this.userViews = new HashMap<>();
        this.familiesData = new LinkedHashMap<>();

        final String orphanageSymbol = StackOrphanage.ORPHANAGE;
        final String orphanISIN = Constants.createISINForSymbol(orphanageSymbol);
        final InstrumentID instID = new InstrumentID(orphanISIN, CCY.USD, MIC.EEIF);
        final StackUIData orphanageStackData =
                new StackUIData("ui", orphanageSymbol, instID, orphanageSymbol, InstType.UNKNOWN, orphanageSymbol);
        addFamilyUIData(orphanageStackData);
    }

    public void setCommunityManager(final StackCommunityManager communityManager) {

        for (final StackFamilyView familyView : communityViews.values()) {
            familyView.setCommunityManager(communityManager);
        }
    }

    public void setStrategyClient(final String nibblerName, final StackClientHandler cache) {

        for (final StackFamilyView familyView : communityViews.values()) {
            familyView.setStrategyClient(nibblerName, cache);
        }
    }

    public void setFamiliesFilters(final StackCommunity community, final Collection<StackChildFilter> filters) {

        final StackFamilyView view = communityViews.get(community);
        view.setFilter(filters);
    }

    void addTradableSymbol(final String nibblerName, final StackTradableSymbol tradableSymbol) {

        for (final StackFamilyView familyView : communityViews.values()) {
            familyView.addTradableSymbol(nibblerName, tradableSymbol);
        }
    }

    public void setSearchResult(final SearchResult searchResult) {

        for (final StackFamilyView view : communityViews.values()) {
            view.setSearchResult(searchResult);
        }
    }

    void addChildUIData(final StackUIData uiData) {

        for (final StackFamilyView view : communityViews.values()) {
            view.addChildUIData(uiData);
        }
    }

    void updateChildUIData(final StackUIData uiData) {

        for (final StackFamilyView view : communityViews.values()) {
            view.updateChildUIData(uiData);
        }
    }

    void addFamilyUIData(final StackUIData uiData) {

        final FamilyUIData familyUIData = new FamilyUIData(uiData);
        familiesData.put(uiData.symbol, familyUIData);

        for (final StackFamilyView view : communityViews.values()) {
            view.addFamilyUIData(familyUIData);
        }
    }

    void updateFamilyUIData(final StackUIData uiData) {

        final FamilyUIData familyData = familiesData.get(uiData.symbol);

        for (final StackFamilyView view : communityViews.values()) {
            view.updateFamilyUIData(familyData);
        }
    }

    void setConfig(final StackConfigGroup stackConfig) {

        for (final StackFamilyView view : communityViews.values()) {
            view.setConfig(stackConfig);
        }
    }

    @Override
    public boolean updateRelationship(final String source, final long relationshipID, final String childSymbol, final String parentSymbol,
            final double bidPriceOffset, final double bidQtyMultiplier, final double askPriceOffset, final double askQtyMultiplier,
            final int familyToChildRatio) {

        for (final StackFamilyView view : communityViews.values()) {

            view.updateRelationship(childSymbol, parentSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier,
                    familyToChildRatio);
        }
        return true;
    }

    @Override
    public boolean setCommunity(final String source, final String familyName, final StackCommunity community) {

        final FamilyUIData familyData = familiesData.get(familyName);
        familyData.setCommunity(community);

        for (final StackFamilyView view : communityViews.values()) {
            view.updateFamilyUIData(familyData);
        }

        return true;
    }

    @Override
    public boolean killFamily(final String source, final String familyName) {
        // NO-OP
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }

    public void symbolSelected(final SymbolSelection symbolSelection) {

        for (final StackFamilyView asylum : communityViews.values()) {
            asylum.symbolSelected(symbolSelection);
        }
    }

    public void setChildStackEnabled(final String source, final String familyName, final BookSide side, final boolean isEnabled) {

        for (final StackCommunity primaryCommunity : primaryCommunities) {
            final StackFamilyView familyView = communityViews.get(primaryCommunity);
            familyView.setChildStackEnabled(source, familyName, side, isEnabled);
        }
    }

    public void stopChild(final String source, final String otcSymbol, final BookSide side) {

        for (final StackCommunity primaryCommunity : primaryCommunities) {
            final StackFamilyView familyView = communityViews.get(primaryCommunity);
            familyView.stopChild(otcSymbol, side);
        }
    }

    void setMetadata(final String source, final String parentSymbol, final String uiName) {

        for (final StackFamilyView familyView : communityViews.values()) {
            familyView.setMetadata(parentSymbol, uiName);
        }
    }

    void setStrategyRunnableForDate(final String source, final String parentSymbol, final Date date, final boolean isRunnable) {
        for (final StackFamilyView familyView : communityViews.values()) {
            familyView.setStrategyRunnableForDate(parentSymbol, date, isRunnable);
        }
    }

    public void autoFamily(final ETFDef etfDef) {
        final StackCommunity community = StackCommunity.getForIndexType(etfDef.indexDef.indexType);
        if (communityViews.containsKey(community)) {
            communityViews.get(community).bufferETFDef(etfDef);
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            final StackFamilyView handler = userViews.get(webMsg.getOutboundChannel());
            if (null != handler) {
                handler.removeUI(webMsg.getClient().getUserName(), (WebSocketDisconnected) webMsg);
            }
        }
    }

    private void inboundData(final WebSocketInboundData msg) {

        uiLogger.write("stackFamilyPresenter", msg);

        final Publisher<WebSocketOutboundData> outChannel = msg.getOutboundChannel();
        final String username = msg.getClient().getUserName();
        final String data = msg.getData();

        final String[] cmdParts = data.split(",");

        if ("subscribeFamily".equals(cmdParts[0])) {
            StackCommunity unit;
            if ("DEFAULT".equals(cmdParts[1])) {
                unit = getDefaultCommunity(primaryCommunities);
            } else {
                try {
                    unit = StackCommunity.get(cmdParts[1]);
                } catch (final IllegalArgumentException e) {
                    unit = null;
                }
            }
            if (null != unit && communityViews.containsKey(unit)) {
                final StackFamilyView familyView = communityViews.get(unit);
                userViews.put(outChannel, familyView);
                final boolean isLazy = 3 <= cmdParts.length && "Lazy".equals(cmdParts[2]);
                familyView.addUI(username, isLazy, outChannel);
            }
        } else if ("subscribeNewFamily".equals(cmdParts[0])) {
            StackCommunity unit;
            if ("DEFAULT".equals(cmdParts[1])) {
                unit = getDefaultCommunity(primaryCommunities);
            } else {
                try {
                    unit = StackCommunity.get(cmdParts[1]);
                } catch (final IllegalArgumentException e) {
                    unit = null;
                }
            }
            if (null != unit && communityViews.containsKey(unit)) {
                final StackFamilyView familyView = communityViews.get(unit);
                userViews.put(outChannel, familyView);
                final boolean isLazy = 3 <= cmdParts.length && "Lazy".equals(cmdParts[2]);
                familyView.addUINew(username, isLazy, outChannel);
            }
        } else {

            final StackFamilyView view = userViews.get(outChannel);
            if (null != view) {
                view.handleWebMsg(msg);
            }
        }
    }

    private static StackCommunity getDefaultCommunity(final Set<StackCommunity> primaryCommunities) {
        if (primaryCommunities.contains(StackCommunity.FUTURE)) {
            return StackCommunity.FUTURE;
        } else {
            return StackCommunity.DM;
        }
    }
}
