package com.drwtrading.london.reddal.stacks.strategiesUI;

import com.drwtrading.london.eeif.stack.transport.data.strategy.StackStrategy;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;

public class StackStrategiesNibblerView {

    private static final String SOURCE = "STRATEGIES_UI";

    private final String nibblerName;
    private final WebSocketViews<IStackStrategiesUI> views;

    private final Map<String, InstrumentID> instIDs;
    private final LongMap<StackStrategy> strategies;

    private StackClientHandler strategyClient;

    public StackStrategiesNibblerView(final String nibblerName) {

        this.nibblerName = nibblerName;

        this.views = WebSocketViews.create(IStackStrategiesUI.class, this);

        this.instIDs = new HashMap<>();
        this.strategies = new LongMap<>();
    }

    public void addInstIDs(final String symbol, final InstrumentID instID) {
        instIDs.put(symbol, instID);
    }

    public void setStrategyClient(final StackClientHandler cache) {
        this.strategyClient = cache;
    }

    public void strategyCreated(final StackStrategy strategy) {
        strategyUpdated(strategy);
    }

    public void strategyUpdated(final StackStrategy strategy) {

        strategies.put(strategy.getStrategyID(), strategy);
        sendLine(views.all(), strategy);
    }

    public void addUI(final Publisher<WebSocketOutboundData> channel) {

        final IStackStrategiesUI newView = views.get(channel);

        for (final LongMapNode<StackStrategy> configNode : strategies) {

            final StackStrategy config = configNode.getValue();
            sendLine(newView, config);
        }
    }

    private static void sendLine(final IStackStrategiesUI viewer, final StackStrategy strategy) {

        final InstrumentID quoteInstID = strategy.getInstID();
        final InstrumentID leanInstID = strategy.getLeanInstID();

        viewer.setRow(strategy.getStrategyID(), strategy.getSymbol(), quoteInstID.isin, quoteInstID.ccy.name(), quoteInstID.mic.name(),
                strategy.getLeanSymbol(), leanInstID.isin, leanInstID.ccy.name(), leanInstID.mic.name(),
                strategy.isQuoteInstDefEventAvailable(), strategy.isQuoteBookAvailable(), strategy.isLeanBookAvailable(),
                strategy.isFXAvailable(), strategy.getSelectedConfigType().name());
    }

    public void serverConnectionLost(final String nibblerName) {
        if (nibblerName.equals(this.nibblerName)) {
            strategies.clear();
            views.all().removeAll();
        }
    }

    public void removeUI(final WebSocketDisconnected disconnected) {
        views.unregister(disconnected);
    }

    void onMessage(final WebSocketInboundData msg) {
        views.invoke(msg);
    }

    @FromWebSocketView
    public void submitSymbol(final String quoteSymbol, final String leanSymbol) {

        if (null != strategyClient) {

            final InstrumentID quoteInstId = instIDs.get(quoteSymbol);
            final InstrumentID leanInstID = instIDs.get(leanSymbol);

            if (null != quoteInstId && null != leanInstID) {
                strategyClient.createStrategy(quoteSymbol, quoteInstId, leanSymbol, leanInstID);
                strategyClient.batchComplete();
            }
        }
    }

    @FromWebSocketView
    public void checkInst(final String type, final String symbol, final WebSocketInboundData data) {

        final IStackStrategiesUI ui = views.get(data.getOutboundChannel());

        final InstrumentID instID = instIDs.get(symbol);

        if (null == instID) {
            ui.noInstID(type);
        } else {
            ui.setInstID(type, instID.isin, instID.ccy.name(), instID.mic.name());
        }
    }
}
