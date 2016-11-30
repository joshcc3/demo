package com.drwtrading.london.reddal.stacks.strategiesUI;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.utils.marketData.InstrumentID;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;

public class StackStrategiesUIRouter {

    private final Map<String, StackStrategiesNibblerView> nibblerHandlers;
    private final Map<Publisher<WebSocketOutboundData>, StackStrategiesNibblerView> nibblerHandlerForUI;

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;

    public StackStrategiesUIRouter(final FiberBuilder logFiber, final UILogger uiLogger) {

        this.nibblerHandlers = new HashMap<>();
        this.nibblerHandlerForUI = new HashMap<>();

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;
    }

    public void addInstID(final String symbol, final InstrumentID instID) {
        for (final StackStrategiesNibblerView view : nibblerHandlers.values()) {
            view.addInstIDs(symbol, instID);
        }
    }

    public StackStrategiesNibblerView getNibblerHandler(final String nibblerName) {

        final StackStrategiesNibblerView existing = nibblerHandlers.get(nibblerName);

        if (null != existing) {
            return existing;
        } else {
            final StackStrategiesNibblerView nibblerHandler = new StackStrategiesNibblerView(nibblerName);
            nibblerHandlers.put(nibblerName, nibblerHandler);
            return nibblerHandler;
        }
    }

    @Subscribe
    public void onConnected(final WebSocketConnected connected) {
        // no-op
    }

    @Subscribe
    public void onDisconnected(final WebSocketDisconnected disconnected) {

        final StackStrategiesNibblerView view = nibblerHandlerForUI.remove(disconnected.getOutboundChannel());
        if (null != view) {
            view.removeUI(disconnected);
        }
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {

        logFiber.execute(() -> uiLogger.write("StackStrategies", msg));

        final String data = msg.getData();
        final String[] args = data.split(",");
        if ("subscribe-nibbler".equals(args[0])) {
            if (1 < args.length) {
                final String nibbler = args[1];
                if (!nibbler.isEmpty()) {
                    final StackStrategiesNibblerView nibblerHandler = getNibblerHandler(nibbler);
                    nibblerHandlerForUI.put(msg.getOutboundChannel(), nibblerHandler);
                    nibblerHandler.addUI(msg.getOutboundChannel());
                }
            }
        } else {
            final StackStrategiesNibblerView nibblerHandler = nibblerHandlerForUI.get(msg.getOutboundChannel());
            if (null != nibblerHandler) {
                nibblerHandler.onMessage(msg);
            }
        }
    }
}
