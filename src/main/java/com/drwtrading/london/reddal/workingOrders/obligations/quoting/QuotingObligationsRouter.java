package com.drwtrading.london.reddal.workingOrders.obligations.quoting;

import com.drwtrading.london.eeif.nibbler.transport.data.tradingData.QuotingState;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
import com.drwtrading.london.eeif.stack.manager.relations.StackCommunity;
import com.drwtrading.london.eeif.utils.io.SelectIO;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class QuotingObligationsRouter {

    private final Map<Publisher<WebSocketOutboundData>, QuotingObligationsPresenter> userViews;
    private final Map<String, QuotingObligationsPresenter> communityViews;
    private final QuotingObligationsPresenter dmView;
    private final EnumMap<StackCommunity, QuotingObligationsPresenter> communityPresenters;

    public QuotingObligationsRouter(final SelectIO uiSelectIO, final UILogger webLog) {
        userViews = new HashMap<>();
        communityViews = new HashMap<>();

        dmView = new QuotingObligationsPresenter(StackCommunity.DM, uiSelectIO, webLog);
        final QuotingObligationsPresenter fiView = new QuotingObligationsPresenter(StackCommunity.FI, uiSelectIO, webLog);

        communityPresenters = new EnumMap<>(StackCommunity.class);
        for (final StackCommunity community : StackCommunity.values()) {
            if (StackCommunity.FI == community) {
                communityPresenters.put(community, fiView);
            } else {
                communityPresenters.put(community, dmView);
            }
        }
    }

    public void setSymbol(final StackCommunity community, final String symbol) {
        final QuotingObligationsPresenter viewForCommunity = communityPresenters.get(community);
        communityViews.put(symbol, viewForCommunity);
    }

    @FromWebSocketView
    public void everythingOn(final WebSocketInboundData inboundData) {
        final QuotingObligationsPresenter quotingObligationsPresenter = userViews.get(inboundData.getOutboundChannel());
        quotingObligationsPresenter.everythingOn(inboundData);
    }

    @FromWebSocketView
    public void everythingOff(final WebSocketInboundData inboundData) {
        final QuotingObligationsPresenter quotingObligationsPresenter = userViews.get(inboundData.getOutboundChannel());
        quotingObligationsPresenter.everythingOff(inboundData);
    }

    @FromWebSocketView
    public void startStrategy(final String symbol, final WebSocketInboundData inboundData) {
        final QuotingObligationsPresenter quotingObligationsPresenter = userViews.get(inboundData.getOutboundChannel());
        quotingObligationsPresenter.startStrategy(symbol, inboundData);
    }

    @FromWebSocketView
    public void setEnabledState(final String symbol, final WebSocketInboundData inboundData) {
        final QuotingObligationsPresenter quotingObligationsPresenter = userViews.get(inboundData.getOutboundChannel());
        quotingObligationsPresenter.setEnabledState(symbol);
    }

    @FromWebSocketView
    public void stopStrategy(final String symbol, final WebSocketInboundData inboundData) {
        final QuotingObligationsPresenter quotingObligationsPresenter = userViews.get(inboundData.getOutboundChannel());
        quotingObligationsPresenter.stopStrategy(symbol);
    }

    public void enableQuotes(final QuoteObligationsEnableCmd quoteObligationsEnableCmd) {
        final QuotingObligationsPresenter presenter = communityPresenters.get(quoteObligationsEnableCmd.community);
        presenter.enableQuotes(quoteObligationsEnableCmd);
    }

    public void setNibblerHandler(final String nibblerName, final NibblerClientHandler nibblerHandler) {
        for (final QuotingObligationsPresenter presenter : communityPresenters.values()) {
            presenter.setNibblerHandler(nibblerName, nibblerHandler);
        }
    }

    public void setQuotingState(final String sourceNibbler, final QuotingState quotingState) {
        final String symbol = quotingState.getSymbol();
        final QuotingObligationsPresenter presenter = communityViews.getOrDefault(symbol, dmView);
        presenter.setQuotingState(sourceNibbler, quotingState);
    }

    public void setNibblerDisconnected(final String sourceNibbler) {
        for (final QuotingObligationsPresenter presenter : communityPresenters.values()) {
            presenter.setNibblerDisconnected(sourceNibbler);
        }
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketConnected) {

        } else if (webMsg instanceof WebSocketDisconnected) {

            onDisconnected((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            onMessage((WebSocketInboundData) webMsg);
        }
    }

    private void onDisconnected(final WebSocketDisconnected disconnected) {
        userViews.get(disconnected.getOutboundChannel()).onDisconnected(disconnected);
    }

    public void onMessage(final WebSocketInboundData msg) {

        final String[] cmdParts = msg.getData().split(",");
        if("subscribeToCommunity".equals(cmdParts[0])) {
            subscribeToCommunity(cmdParts[1], msg);
        } else {
            final QuotingObligationsPresenter presenter1 = userViews.get(msg.getOutboundChannel());
            if(null != presenter1) {
                presenter1.onMessage(msg);
            }
        }
    }

    private void subscribeToCommunity(final String communityStr, final WebSocketInboundData msg) {
        final StackCommunity community = StackCommunity.get(communityStr);
        if(null != community) {
            final QuotingObligationsPresenter presenter = communityPresenters.get(community);
            userViews.put(msg.getOutboundChannel(), presenter);
            presenter.onConnected(msg);
        }
    }

}
