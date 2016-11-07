package com.drwtrading.london.reddal.stacks.configui;

import com.drwtrading.jetlang.autosubscribe.Subscribe;
import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.util.HashMap;
import java.util.Map;

public class StackConfigUIRouter {

    private final Map<String, StackConfigNibblerView> nibblerHandlers;
    private final Map<Publisher<WebSocketOutboundData>, StackConfigNibblerView> nibblerHandlerForUI;

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;

    public StackConfigUIRouter(final FiberBuilder logFiber, final UILogger uiLogger) {

        this.nibblerHandlers = new HashMap<>();
        this.nibblerHandlerForUI = new HashMap<>();

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;
    }

    public StackConfigNibblerView getNibblerHandler(final String nibblerName) {

        final StackConfigNibblerView existing = nibblerHandlers.get(nibblerName);

        if (null != existing) {
            return existing;
        } else {
            final StackConfigNibblerView nibblerHandler = new StackConfigNibblerView(nibblerName);
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

        nibblerHandlerForUI.remove(disconnected.getOutboundChannel()).removeUI(disconnected);
    }

    @Subscribe
    public void onMessage(final WebSocketInboundData msg) {

        logFiber.execute(() -> uiLogger.write("StackConfig", msg));

        final String data = msg.getData();
        final String[] args = data.split(",");
        System.out.println(data);
        if ("subscribe-nibbler".equals(args[0])) {
            final String nibbler = args[1];
            if (!nibbler.isEmpty()) {
                final StackConfigNibblerView nibblerHandler = getNibblerHandler(nibbler);
                nibblerHandlerForUI.put(msg.getOutboundChannel(), nibblerHandler);
                nibblerHandler.addUI(msg.getOutboundChannel());
            }
        } else {
            final StackConfigNibblerView nibblerHandler = nibblerHandlerForUI.get(msg.getOutboundChannel());
            nibblerHandler.onMessage(msg);
        }
    }
}
