package com.drwtrading.london.reddal.blotter;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.ANibblerSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerDoubleSafety;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerOMSEnabledState;
import com.drwtrading.london.eeif.nibbler.transport.data.safeties.NibblerSafety;
import com.drwtrading.london.eeif.nibbler.transport.io.NibblerClientHandler;
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
    private final Map<String, NibblerClientHandler> nibblerClients;

    private final DecimalFormat longDF;
    private final DecimalFormat doubleDF;

    private int lastRowID;

    public SafetiesBlotterPresenter(final FiberBuilder logFiber, final UILogger uiLogger) {

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;

        this.views = WebSocketViews.create(ISafetiesBlotterView.class, this);

        this.nibblers = new HashMap<>();
        this.nibblerClients = new HashMap<>();

        this.longDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 0);
        this.doubleDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2);

        this.lastRowID = -1;
    }

    public void setNibblerClient(final String nibbler, final NibblerClientHandler client) {

        nibblerClients.put(nibbler, client);
    }

    public void setNibblerConnected(final String nibbler, final boolean isConnected) {

        final SafetiesBlotterBlock block = nibblers.get(nibbler);
        if (null == block) {
            final SafetiesBlotterBlock newBlock = new SafetiesBlotterBlock(longDF, nibbler, isConnected);
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

    void addOMS(final String source, final NibblerOMSEnabledState omsEnabledState) {

        final SafetiesBlotterBlock block = nibblers.get(source);
        final SafetiesOMSRow omsRow = block.createOMSEnabledState(++lastRowID, omsEnabledState);
        views.all().setOMS(omsRow.id, omsRow.source, omsRow.remoteOMSID, omsRow.omsName, omsRow.isEnabled, omsRow.stateText);
    }

    void updateOMSEnabledState(final String source, final NibblerOMSEnabledState omsEnabledState) {

        final SafetiesBlotterBlock block = nibblers.get(source);
        final SafetiesOMSRow omsRow = block.udpateOMSEnabledState(omsEnabledState);
        views.all().setOMS(omsRow.id, omsRow.source, omsRow.remoteOMSID, omsRow.omsName, omsRow.isEnabled, omsRow.stateText);
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
                currentLevel = doubleDF.format(((NibblerDoubleSafety) safety).getCurrent());
                break;
            }
            default: {
                throw new IllegalArgumentException("Safety type [" + safety.getSafetyType() + "] not supported.");
            }
        }

        final SafetiesBlotterRow row = block.createSafety(++lastRowID, safety, currentLevel);

        views.all().setRow(row.id, row.source, row.remoteSafetyID, row.safetyName, row.limit, row.warningLevel, row.currentLevel,
                row.lastSymbol, row.isEditable, row.isWarning, row.isError);
    }

    void updateSafety(final String source, final NibblerSafety safety) {

        final SafetiesBlotterBlock block = nibblers.get(source);
        final String currentLevel = longDF.format(safety.getCurrent());
        final SafetiesBlotterRow row = block.updateSafety(safety, currentLevel);

        views.all().setRow(row.id, row.source, row.remoteSafetyID, row.safetyName, row.limit, row.warningLevel, row.currentLevel,
                row.lastSymbol, row.isEditable, row.isWarning, row.isError);
    }

    void updateSafety(final String source, final NibblerDoubleSafety safety) {

        final SafetiesBlotterBlock block = nibblers.get(source);
        final String currentLevel = doubleDF.format(safety.getCurrent());
        final SafetiesBlotterRow row = block.updateSafety(safety, currentLevel);

        views.all().setRow(row.id, row.source, row.remoteSafetyID, row.safetyName, row.limit, row.warningLevel, row.currentLevel,
                row.lastSymbol, row.isEditable, row.isWarning, row.isError);
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
        } else if ("setLimit".equals(args[0])) {
            setLimit(args[1], Integer.parseInt(args[2]), args[3]);
        } else if ("enableOMS".equals(args[0])) {
            enableOMS(args[1]);
        } else if ("sendDebug".equals(args[0])) {
            sendDebug(args[1], args[2]);
        } else {
            throw new IllegalArgumentException("Message [" + msg + "] not expected.");
        }
    }

    private void addUI(final Publisher<WebSocketOutboundData> channel) {

        final ISafetiesBlotterView newView = views.get(channel);

        for (final Map.Entry<String, SafetiesBlotterBlock> nibblerBlock : nibblers.entrySet()) {

            final SafetiesBlotterBlock block = nibblerBlock.getValue();
            newView.setNibblerConnected(block.source, block.isConnected);

            for (final LongMapNode<SafetiesOMSRow> omsNode : block.omsEnabledStates) {

                final SafetiesOMSRow row = omsNode.getValue();
                newView.setOMS(row.id, row.source, row.remoteOMSID, row.omsName, row.isEnabled, row.stateText);
            }

            for (final LongMapNode<SafetiesBlotterRow> rowNode : block.safeties) {

                final SafetiesBlotterRow row = rowNode.getValue();
                newView.setRow(row.id, row.source, row.remoteSafetyID, row.safetyName, row.limit, row.warningLevel, row.currentLevel,
                        row.lastSymbol, row.isEditable, row.isWarning, row.isError);
            }
        }
    }

    private void setLimit(final String nibblerName, final int remoteSafetyID, final String limitStr) {

        try {
            final NibblerClientHandler client = nibblerClients.get(nibblerName);
            final long limit = doubleDF.parse(limitStr).longValue();

            client.setSafetyLimit(remoteSafetyID, limit);
            client.batchComplete();
        } catch (final Exception e) {
            throw new RuntimeException("Could not handle [" + nibblerName + "] set limit [" + remoteSafetyID + "] to [" + limitStr + "].",
                    e);
        }
    }

    private void enableOMS(final String nibblerName) {
        final NibblerClientHandler client = nibblerClients.get(nibblerName);
        client.enableAllOMS();
        client.batchComplete();
    }

    private void sendDebug(final String nibblerName, final String text) {
        final NibblerClientHandler client = nibblerClients.get(nibblerName);
        client.sendDebugMsg(text);
        client.batchComplete();
    }
}
