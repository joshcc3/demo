package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.transport.cache.families.IStackRelationshipListener;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;

public class StackFamilyPresenter implements IStackRelationshipListener {

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;

    private final StackFamilyView familyView;
    private final StackFamilyView asylumView;

    private final Map<Publisher<WebSocketOutboundData>, StackFamilyView> userViews;

    public StackFamilyPresenter(final FiberBuilder logFiber, final UILogger uiLogger,
            final SpreadContractSetGenerator contractSetGenerator) {

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;

        this.familyView = new StackFamilyView(contractSetGenerator, false);
        this.asylumView = new StackFamilyView(contractSetGenerator, true);

        this.userViews = new HashMap<>();

    }

    public void setCommunityManager(final StackCommunityManager communityManager) {
        familyView.setCommunityManager(communityManager);
        asylumView.setCommunityManager(communityManager);
    }

    public void setStrategyClient(final String nibblerName, final StackClientHandler cache) {
        familyView.setStrategyClient(nibblerName, cache);
        asylumView.setStrategyClient(nibblerName, cache);
    }

    public void setFilter(final StackChildFilter newFilter) {
        familyView.setFilter(newFilter);
        asylumView.setFilter(newFilter);
    }

    public void setSearchResult(final SearchResult searchResult) {
        familyView.setSearchResult(searchResult);
        asylumView.setSearchResult(searchResult);
    }

    void addChildUIData(final StackUIData uiData) {
        familyView.addChildUIData(uiData);
        asylumView.addChildUIData(uiData);
    }

    void updateChildUIData(final StackUIData uiData) {
        familyView.updateChildUIData(uiData);
        asylumView.updateChildUIData(uiData);
    }

    void addFamily(final String familyName) {
        familyView.addFamily(familyName);
        asylumView.addFamily(familyName);
    }

    void addFamilyUIData(final StackUIData uiData) {
        familyView.addFamilyUIData(uiData);
        asylumView.addFamilyUIData(uiData);
    }

    void updateFamilyUIData(final StackUIData uiData) {
        familyView.updateFamilyUIData(uiData);
        asylumView.updateFamilyUIData(uiData);
    }

    void setConfig(final StackConfigGroup stackConfig) {
        familyView.setConfig(stackConfig);
        asylumView.setConfig(stackConfig);
    }

    @Override
    public boolean updateRelationship(final String source, final long relationshipID, final String childSymbol, final String parentSymbol,
            final double bidPriceOffset, final double bidQtyMultiplier, final double askPriceOffset, final double askQtyMultiplier,
            final int familyToChildRatio) {

        familyView.updateRelationship(source, relationshipID, childSymbol, parentSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset,
                askQtyMultiplier, familyToChildRatio);
        asylumView.updateRelationship(source, relationshipID, childSymbol, parentSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset,
                askQtyMultiplier, familyToChildRatio);
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }

    public void symbolSelected(final SymbolSelection symbolSelection) {
        familyView.symbolSelected(symbolSelection);
        asylumView.symbolSelected(symbolSelection);
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) webMsg);

        } else if (webMsg instanceof WebSocketDisconnected) {

            final StackFamilyView handler = userViews.get(webMsg.getOutboundChannel());
            handler.removeUI(webMsg.getClient().getUserName(), (WebSocketDisconnected) webMsg);
        }
    }

    private void inboundData(final WebSocketInboundData msg) {

        logFiber.execute(() -> uiLogger.write("stackFamilyPresenter", msg));

        final Publisher<WebSocketOutboundData> outChannel = msg.getOutboundChannel();
        final String username = msg.getClient().getUserName();
        final String data = msg.getData();

        if ("subscribeFamily".equals(data)) {

            userViews.put(outChannel, familyView);
            familyView.addUI(username, outChannel);

        } else if ("subscribeAsylum".equals(data)) {

            userViews.put(outChannel, asylumView);
            asylumView.addUI(username, outChannel);

        } else {

            final StackFamilyView view = userViews.get(outChannel);
            view.handleWebMsg(msg);
        }
    }
}
