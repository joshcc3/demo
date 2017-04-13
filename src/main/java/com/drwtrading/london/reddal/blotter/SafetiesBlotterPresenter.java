package com.drwtrading.london.reddal.blotter;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerDoubleSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerSafety;
import com.drwtrading.london.eeif.utils.collections.LongMapNode;
import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;

public class SafetiesBlotterPresenter {

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;

    private final WebSocketViews<ISafetiesBlotterView> views;

    private final Map<String, SafetiesBlotterBlock> nibblers;

    private final DecimalFormat longDF;
    private final DecimalFormat doubleDF;

    private int lastSafetyRowID;

    public SafetiesBlotterPresenter(final FiberBuilder logFiber, final UILogger uiLogger) {

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;

        this.views = WebSocketViews.create(ISafetiesBlotterView.class, this);

        this.nibblers = new HashMap<>();

        this.longDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 0);
        this.doubleDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2);

        this.lastSafetyRowID = -1;
    }

    public void setNibblerConnected(final String nibbler, final boolean isConnected) {

        final SafetiesBlotterBlock block = nibblers.get(nibbler);
        if (null == block) {
            final SafetiesBlotterBlock newBlock = new SafetiesBlotterBlock(nibbler, isConnected);
            nibblers.put(nibbler, newBlock);
        } else {
            block.isConnected = isConnected;
            if (!isConnected) {

                block.clear();
                views.all().removeAllRows(nibbler);
            }
        }

        views.all().setNibblerConnected(nibbler, isConnected);
    }

    void setSafety(final String source, final ANibblerSafety<?> safety) {

        final SafetiesBlotterBlock block = nibblers.get(source);

        final String currentLevel;
        switch (safety.getSafetyType()) {
            case LONG: {
                currentLevel = longDF.format(((NibblerSafety) safety).getCurrent());
                break;
            }
            case DOUBLE: {
                currentLevel = doubleDF.format(((NibblerSafety) safety).getCurrent());
                break;
            }
            default: {
                throw new IllegalArgumentException("Safety type [" + safety.getSafetyType() + "] not supported.");
            }
        }

        final SafetiesBlotterRow row = block.setSafety(++lastSafetyRowID, safety, currentLevel);

        views.all().setRow(row.id, row.source, row.safetyName, row.limit, row.warningLevel, row.currentLevel, row.lastSymbol, row.isWarning,
                row.isError);
    }

    void updateSafety(final String source, final NibblerSafety safety) {

        final SafetiesBlotterBlock block = nibblers.get(source);
        final String currentLevel = longDF.format(safety.getCurrent());
        final SafetiesBlotterRow row = block.updateSafety(safety, currentLevel);

        views.all().setRow(row.id, row.source, row.safetyName, row.limit, row.warningLevel, row.currentLevel, row.lastSymbol, row.isWarning,
                row.isError);
    }

    void updateSafety(final String source, final NibblerDoubleSafety safety) {

        final SafetiesBlotterBlock block = nibblers.get(source);
        final String currentLevel = longDF.format(safety.getCurrent());
        final SafetiesBlotterRow row = block.updateSafety(safety, currentLevel);

        views.all().setRow(row.id, row.source, row.safetyName, row.limit, row.warningLevel, row.currentLevel, row.lastSymbol, row.isWarning,
                row.isError);
    }

    public void webControl(final WebSocketControlMessage webMsg) {

        if (webMsg instanceof WebSocketDisconnected) {

            views.unregister((WebSocketDisconnected) webMsg);

        } else if (webMsg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) webMsg);
        }
    }

    private void inboundData(final WebSocketInboundData msg) {

        logFiber.execute(() -> uiLogger.write("Blotter", msg));

        final String data = msg.getData();
        final String[] args = data.split(",");
        if ("subscribe".equals(args[0])) {
            addUI(msg.getOutboundChannel());
        } else {

            throw new IllegalArgumentException("No messages expected.");
        }
    }

    public void addUI(final Publisher<WebSocketOutboundData> channel) {

        final ISafetiesBlotterView newView = views.get(channel);

        for (final Map.Entry<String, SafetiesBlotterBlock> nibblerBlock : nibblers.entrySet()) {

            final SafetiesBlotterBlock block = nibblerBlock.getValue();
            newView.setNibblerConnected(block.source, block.isConnected);

            for (final LongMapNode<SafetiesBlotterRow> rowNode : block.safeties) {

                final SafetiesBlotterRow row = rowNode.getValue();
                newView.setRow(row.id, row.source, row.safetyName, row.limit, row.warningLevel, row.currentLevel, row.lastSymbol,
                        row.isWarning, row.isError);
            }
        }
    }
}
