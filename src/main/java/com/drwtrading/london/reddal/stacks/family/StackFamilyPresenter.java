package com.drwtrading.london.reddal.stacks.family;

import com.drwtrading.london.eeif.stack.manager.relations.StackCommunityManager;
import com.drwtrading.london.eeif.stack.transport.cache.relationships.IStackRelationshipListener;
import com.drwtrading.london.eeif.stack.transport.data.config.StackConfigGroup;
import com.drwtrading.london.eeif.stack.transport.data.symbology.StackTradableSymbol;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.Constants;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.eeif.utils.marketData.book.BookSide;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.ladders.history.SymbolSelection;
import com.drwtrading.london.reddal.symbols.SearchResult;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.reddal.workingOrders.obligations.quoting.QuoteObligationsEnableCmd;
import com.drwtrading.london.reddal.workspace.SpreadContractSetGenerator;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Channel;
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StackFamilyPresenter implements IStackRelationshipListener {

    private final UILogger uiLogger;

    private final StackFamilyView familyView;
    private final Map<String, StackFamilyView> asylums;

    private final Map<Publisher<WebSocketOutboundData>, StackFamilyView> userViews;

    public StackFamilyPresenter(final SelectIO presenterSelectIO, final SelectIO backgroundSelectIO, final UILogger uiLogger,
            final SpreadContractSetGenerator contractSetGenerator, final InstType defaultInstType, final Map<InstType, String> families,
            final Publisher<QuoteObligationsEnableCmd> quotingObligationsCmds) {

        this.uiLogger = uiLogger;

        this.familyView = new StackFamilyView(presenterSelectIO, backgroundSelectIO, contractSetGenerator, false, defaultInstType,
                quotingObligationsCmds);

        families.remove(defaultInstType);

        this.asylums = new HashMap<>();
        for (final Map.Entry<InstType, String> instType : families.entrySet()) {
            final StackFamilyView asylumView =
                    new StackFamilyView(presenterSelectIO, backgroundSelectIO, contractSetGenerator, true, instType.getKey(),
                            Constants::NO_OP);
            asylums.put(instType.getValue(), asylumView);
        }

        this.userViews = new HashMap<>();
    }

    public void setCommunityManager(final StackCommunityManager communityManager) {

        familyView.setCommunityManager(communityManager);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.setCommunityManager(communityManager);
        }
    }

    public void setStrategyClient(final String nibblerName, final StackClientHandler cache) {
        familyView.setStrategyClient(nibblerName, cache);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.setStrategyClient(nibblerName, cache);
        }
    }

    public void setFamiliesFilters(final InstType forInstType, final Collection<StackChildFilter> filters) {

        familyView.setFilter(forInstType, filters);
    }

    public void setAsylumFilters(final InstType forInstType, final String asylumName, final Collection<StackChildFilter> filters) {

        final StackFamilyView view = asylums.get(asylumName);
        view.setFilter(forInstType, filters);
    }

    void addTradableSymbol(final String nibblerName, final StackTradableSymbol tradableSymbol) {

        familyView.addTradableSymbol(nibblerName, tradableSymbol);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.addTradableSymbol(nibblerName, tradableSymbol);
        }
    }

    public void setSearchResult(final SearchResult searchResult) {
        familyView.setSearchResult(searchResult);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.setSearchResult(searchResult);
        }
    }

    void addChildUIData(final StackUIData uiData) {
        familyView.addChildUIData(uiData);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.addChildUIData(uiData);
        }
    }

    void updateChildUIData(final StackUIData uiData) {
        familyView.updateChildUIData(uiData);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.updateChildUIData(uiData);
        }
    }

    void addFamilyUIData(final StackUIData uiData) {
        familyView.addFamilyUIData(uiData);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.addFamilyUIData(uiData);
        }
    }

    void updateFamilyUIData(final StackUIData uiData) {
        familyView.updateFamilyUIData(uiData);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.updateFamilyUIData(uiData);
        }
    }

    void setConfig(final StackConfigGroup stackConfig) {
        familyView.setConfig(stackConfig);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.setConfig(stackConfig);
        }
    }

    @Override
    public boolean updateRelationship(final String source, final long relationshipID, final String childSymbol, final String parentSymbol,
            final double bidPriceOffset, final double bidQtyMultiplier, final double askPriceOffset, final double askQtyMultiplier,
            final int familyToChildRatio) {

        familyView.updateRelationship(source, relationshipID, childSymbol, parentSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset,
                askQtyMultiplier, familyToChildRatio);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.updateRelationship(source, relationshipID, childSymbol, parentSymbol, bidPriceOffset, bidQtyMultiplier, askPriceOffset,
                    askQtyMultiplier, familyToChildRatio);
        }
        return true;
    }

    @Override
    public boolean batchComplete() {
        return true;
    }

    public void symbolSelected(final SymbolSelection symbolSelection) {
        familyView.symbolSelected(symbolSelection);
        for (final StackFamilyView asylum : asylums.values()) {
            asylum.symbolSelected(symbolSelection);
        }
    }

    public void setChildStackEnabled(final String source, final String familyName, final BookSide side, final boolean isEnabled) {
        familyView.setChildStackEnabled(source, familyName, side, isEnabled);
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

        if ("subscribeFamily".equals(data)) {

            userViews.put(outChannel, familyView);
            familyView.addUI(username, outChannel);

        } else if ("subscribeAsylum".equals(cmdParts[0])) {

            final StackFamilyView asylumView = cmdParts.length < 2 ? null : asylums.get(cmdParts[1]);
            if (null != asylumView) {
                userViews.put(outChannel, asylumView);
                asylumView.addUI(username, outChannel);
            }
        } else {

            final StackFamilyView view = userViews.get(outChannel);
            view.handleWebMsg(msg);
        }
    }
}
