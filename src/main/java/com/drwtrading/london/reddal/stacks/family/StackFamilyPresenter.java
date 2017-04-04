package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.transport.cache.families.IStackRelationshipListener;
import com.drwtrading.london.eeif.stack.transport.data.types.StackConfigType;
import com.drwtrading.london.eeif.stack.transport.data.types.StackType;
import com.drwtrading.london.eeif.utils.collections.MapUtils;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.FutureConstant;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;

public class StackFamilyPresenter implements IStackRelationshipListener {

    private static final String SOURCE_UI = "FAMILY_ADMIN_UI";

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;

    private final WebSocketViews<IStackFamilyUI> views;

    private final Map<String, NavigableMap<String, StackUIRelationship>> families;
    private final Map<String, StackUIData> parentData;
    private final Set<String> children;

    private final Map<String, SearchResult> searchResults;

    private StackCommunityManager communityManager;

    public StackFamilyPresenter(final FiberBuilder logFiber, final UILogger uiLogger) {

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;

        this.views = WebSocketViews.create(IStackFamilyUI.class, this);

        this.families = new HashMap<>();
        this.parentData = new HashMap<>();
        this.children = new HashSet<>();

        this.searchResults = new HashMap<>();
    }

    public void setCommunityManager(final StackCommunityManager communityManager) {
        this.communityManager = communityManager;
    }

    void addFamily(final String familyName) {

        families.put(familyName, new TreeMap<>());
        views.all().addFamily(familyName);
    }

    void addUIData(final StackUIData uiData) {

        parentData.put(uiData.familyName, uiData);
        updateUIData(views.all(), uiData);
    }

    void updateUIData(final StackUIData uiData) {
        updateUIData(views.all(), uiData);
    }

    private static void updateUIData(final IStackFamilyUI view, final StackUIData uiData) {
        view.setParentData(uiData.familyName, uiData.getBidPriceOffsetBPS(), uiData.getAskPriceOffsetBPS(), uiData.getSelectedConfigType(),
                uiData.isStackEnabled(BookSide.BID, StackType.PICARD), uiData.isStackEnabled(BookSide.BID, StackType.QUOTER),
                uiData.isStackEnabled(BookSide.ASK, StackType.PICARD), uiData.isStackEnabled(BookSide.ASK, StackType.QUOTER));
    }

    @Override
    public boolean updateRelationship(final String source, final long relationshipID, final String childSymbol, final String parentSymbol,
            final double bidPriceOffset, final double bidQtyMultiplier, final double askPriceOffset, final double askQtyMultiplier) {

        for (final Map<String, StackUIRelationship> children : families.values()) {
            children.remove(childSymbol);
        }

        final Map<String, StackUIRelationship> familyChildren = MapUtils.getNavigableMap(families, parentSymbol);

        final StackUIRelationship newRelationship =
                new StackUIRelationship(childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier);
        familyChildren.put(childSymbol, newRelationship);

        children.add(childSymbol);

        views.all().setChild(parentSymbol, childSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset, askQtyMultiplier);
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }

    public void setSearchResult(final SearchResult searchResult) {
        searchResults.put(searchResult.symbol, searchResult);
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketDisconnected) {

            views.unregister((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) webMsg);
        }
    }

    private void inboundData(final WebSocketInboundData msg) {

        logFiber.execute(() -> uiLogger.write("StackConfig", msg));

        final String data = msg.getData();
        if ("subscribe".equals(data)) {
            addUI(msg.getOutboundChannel());
        } else {
            views.invoke(msg);
        }
    }

    private void addUI(final Publisher<WebSocketOutboundData> channel) {

        final IStackFamilyUI newView = views.get(channel);

        for (final Map.Entry<String, NavigableMap<String, StackUIRelationship>> family : families.entrySet()) {

            final String familyName = family.getKey();
            newView.addFamily(familyName);

            for (final StackUIRelationship child : family.getValue().values()) {

                newView.setChild(familyName, child.childSymbol, child.bidPriceOffsetBPS, child.bidQtyMultiplier, child.askPriceOffsetBPS,
                        child.askQtyMultiplier);
            }
        }

        for (final StackUIData uiData : parentData.values()) {
            updateUIData(newView, uiData);
        }
    }

    @FromWebSocketView
    public void checkFamilyInst(final String family, final String resultFieldID, final WebSocketInboundData data) {

        final IStackFamilyUI ui = views.get(data.getOutboundChannel());

        final SearchResult searchResult = searchResults.get(family);

        if (null == searchResult) {
            ui.clearFieldData(resultFieldID);
        } else {
            final InstrumentID instID = searchResult.instID;
            ui.setInstID(instID.isin, instID.ccy.name(), instID.mic.name(), searchResult.instType.name());
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
    public void checkChildExists(final String child, final String resultFieldID, final WebSocketInboundData data) {

        final IStackFamilyUI ui = views.get(data.getOutboundChannel());

        if (children.contains(child)) {

            ui.setFieldData(resultFieldID, "Child available.");
        } else {

            ui.clearFieldData(resultFieldID);
        }
    }

    @FromWebSocketView
    public void createFamily(final String symbol, final WebSocketInboundData data) {

        final SearchResult searchResult = searchResults.get(symbol);
        if (null != searchResult) {
            final String family;
            switch (searchResult.instType) {
                case FUTURE: {
                    family = FutureConstant.getFutureFromSymbol(symbol).name();
                    break;
                }
                case FUTURE_SPREAD: {
                    final String frontMonth = symbol.split("-")[0];
                    family = FutureConstant.getFutureFromSymbol(frontMonth).name();
                    break;
                }
                default: {
                    family = symbol.split(" ")[0];
                }
            }
            final InstrumentID instID = searchResult.instID;
            final InstType instType = searchResult.instType;

            communityManager.createFamily(SOURCE_UI, family, instID, instType);
        }
    }

    @FromWebSocketView
    public void adoptChild(final String family, final String child, final WebSocketInboundData data) {

        if (!families.containsKey(family)) {

            throw new IllegalArgumentException("Parent [" + family + "] is not known. Should it be created first?");
        } else if (!children.contains(child)) {

            throw new IllegalArgumentException("Child [" + child + "] is not known. Has it been created in nibbler?");
        } else {

            communityManager.setRelationship(SOURCE_UI, family, child);
        }
    }

    @FromWebSocketView
    public void setRelationship(final String childSymbol, final String bidPriceOffsetStr, final String bidQtyMultiplierText,
            final String askPriceOffsetStr, final String askQtyMultiplierText, final WebSocketInboundData data) {

        try {
            final double bidPriceOffset = Double.parseDouble(bidPriceOffsetStr);
            final double askPriceOffset = Double.parseDouble(askPriceOffsetStr);
            communityManager.setChildPriceOffsets(SOURCE_UI, childSymbol, bidPriceOffset, askPriceOffset);

            final double bidQtyMultiplier = Double.parseDouble(bidQtyMultiplierText);
            final double askQtyMultiplier = Double.parseDouble(askQtyMultiplierText);
            communityManager.setChildQtyMultipliers(SOURCE_UI, childSymbol, bidQtyMultiplier, askQtyMultiplier);
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
    public void refreshParent(final String parentSymbol, final WebSocketInboundData data) {

        communityManager.refreshParent(parentSymbol);
    }

    @FromWebSocketView
    public void increaseOffset(final String familyName, final String bookSide, final int multiplier, final WebSocketInboundData data) {

        final BookSide side = BookSide.valueOf(bookSide);
        communityManager.increaseOffset(SOURCE_UI, familyName, side, multiplier);
    }

    @FromWebSocketView
    public void selectConfig(final String familyName, final String configType) {

        final StackConfigType stackConfigType = StackConfigType.valueOf(configType);
        communityManager.setParentSelectedConfig(SOURCE_UI, familyName, stackConfigType);
    }

    @FromWebSocketView
    public void setStackEnabled(final String familyName, final String bookSide, final String stack, final boolean isEnabled) {

        final BookSide side = BookSide.valueOf(bookSide);
        final StackType stackType = StackType.valueOf(stack);
        communityManager.setStackEnabled(SOURCE_UI, familyName, side, stackType, isEnabled);
    }

    @FromWebSocketView
    public void setAllStacksEnabled(final String familyName, final boolean isEnabled) {

        for (final BookSide side : BookSide.values()) {
            for (final StackType stackType : StackType.values()) {
                communityManager.setStackEnabled(SOURCE_UI, familyName, side, stackType, isEnabled);
            }
        }
    }
}
