package com.drwtrading.london.reddal.stacks.strategiesUI;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.stack.transport.data.strategy.StackStrategy;
import com.drwtrading.london.eeif.stack.transport.io.StackClientHandler;
import com.drwtrading.london.eeif.utils.collections.LongMap;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import com.google.common.collect.Lists;
import org.jetlang.channels.Publisher;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StackStrategiesPresenter {

    private static final Collection<String> ALLOWED_INST_TYPES =
            Lists.newArrayList(InstType.EQUITY.name(), InstType.DR.name(), InstType.INDEX.name(), InstType.SYNTHETIC.name(), InstType.FUTURE.name());

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;

    private final WebSocketViews<IStackStrategiesUI> views;

    private final Map<String, InstrumentID> instIDs;
    private final Map<String, LongMap<StackStrategy>> nibblerStrategies;

    private final Map<String, StackClientHandler> strategyClients;

    public StackStrategiesPresenter(final FiberBuilder logFiber, final UILogger uiLogger) {

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;

        this.views = WebSocketViews.create(IStackStrategiesUI.class, this);

        this.instIDs = new HashMap<>();
        this.nibblerStrategies = new HashMap<>();

        this.strategyClients = new HashMap<>();
    }

    public void setStrategyClient(final String nibblerName, final StackClientHandler cache) {
        this.strategyClients.put(nibblerName, cache);
        this.nibblerStrategies.put(nibblerName, new LongMap<>());
        views.all().addAvailableNibblers(nibblerStrategies.keySet());
    }

    public void addInstID(final String symbol, final InstrumentID instID) {
        instIDs.put(symbol, instID);
    }

    public void strategyUpdated(final String nibblerName, final StackStrategy strategy) {

        final LongMap<StackStrategy> strategies = nibblerStrategies.get(nibblerName);
        strategies.put(strategy.getStrategyID(), strategy);
        sendLine(views.all(), nibblerName, strategy);
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketDisconnected) {

            views.unregister((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) webMsg);
        }
    }

    private void inboundData(final WebSocketInboundData msg) {

        logFiber.execute(() -> uiLogger.write("StackStrategies", msg));

        final String data = msg.getData();
        final String[] args = data.split(",");
        if ("subscribe".equals(args[0])) {
            addUI(msg.getOutboundChannel());
        } else {
            onMessage(msg);
        }
    }

    public void addUI(final Publisher<WebSocketOutboundData> channel) {

        final IStackStrategiesUI newView = views.get(channel);
        newView.addInstType(ALLOWED_INST_TYPES);
        newView.addAvailableNibblers(nibblerStrategies.keySet());

        for (final Map.Entry<String, LongMap<StackStrategy>> strategies : nibblerStrategies.entrySet()) {

            final String nibblerName = strategies.getKey();
            for (final LongMapNode<StackStrategy> configNode : strategies.getValue()) {

                final StackStrategy config = configNode.getValue();
                sendLine(newView, nibblerName, config);
            }
        }
    }

    private static void sendLine(final IStackStrategiesUI viewer, final String nibbler, final StackStrategy strategy) {

        final InstrumentID quoteInstID = strategy.getInstID();
        final InstrumentID leanInstID = strategy.getLeanInstID();

        viewer.setRow(nibbler, strategy.getStrategyID(), strategy.getSymbol(), quoteInstID.isin, quoteInstID.ccy.name(),
                quoteInstID.mic.name(), strategy.getLeanInstType().name(), strategy.getLeanSymbol(), leanInstID.isin, leanInstID.ccy.name(),
                leanInstID.mic.name(), strategy.isQuoteInstDefEventAvailable(), strategy.isQuoteBookAvailable(),
                strategy.isLeanBookAvailable(), strategy.isFXAvailable(), strategy.isAdditiveAvailable(),
                strategy.getSelectedConfigType().name());
    }

    public void serverConnectionLost(final String nibblerName) {

        nibblerStrategies.get(nibblerName).clear();
        views.all().removeAll(nibblerName);
    }

    void onMessage(final WebSocketInboundData msg) {
        views.invoke(msg);
    }

    @FromWebSocketView
    public void submitSymbol(final String nibblerName, final String quoteSymbol, final String leanInstrumentType, final String leanSymbol) {

        final StackClientHandler strategyClient = strategyClients.get(nibblerName);
        if (null != strategyClient) {

            final InstrumentID quoteInstId = instIDs.get(quoteSymbol);
            final InstType leanInstType = InstType.getInstType(leanInstrumentType);
            final InstrumentID leanInstID = instIDs.get(leanSymbol);

            if (null != quoteInstId && null != leanInstType && null != leanInstID) {
                strategyClient.createStrategy(quoteSymbol, quoteInstId, leanInstType, leanSymbol, leanInstID, "");
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

    @FromWebSocketView
    public void killSymbol(final String nibblerName, final String quoteSymbol) {

        final StackClientHandler strategyClient = strategyClients.get(nibblerName);
        if (null != strategyClient) {
            strategyClient.killStrategy(quoteSymbol);
            strategyClient.batchComplete();
        }
    }

    @FromWebSocketView
    public void killInactiveSymbols(final String nibblerName) {

        final StackClientHandler strategyClient = strategyClients.get(nibblerName);
        if (null != strategyClient) {

            final LongMap<StackStrategy> stacks = nibblerStrategies.get(nibblerName);
            for (final LongMapNode<StackStrategy> stackNode : stacks) {

                final StackStrategy stackStrategy = stackNode.getValue();
                if (!stackStrategy.isQuoteInstDefEventAvailable()) {
                    strategyClient.killStrategy(stackStrategy.getSymbol());
                    strategyClient.batchComplete();
                }
            }
        }
    }
}
