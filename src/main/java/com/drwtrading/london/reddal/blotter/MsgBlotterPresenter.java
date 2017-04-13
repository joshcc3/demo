package com.drwtrading.london.reddal.blotter;

import com.drwtrading.jetlang.builder.FiberBuilder;
import com.drwtrading.london.eeif.nibbler.transport.data.blotter.BlotterLine;
import com.drwtrading.london.eeif.utils.time.DateTimeUtil;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import com.drwtrading.websockets.WebSocketOutboundData;
import org.jetlang.channels.Publisher;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeSet;

public class MsgBlotterPresenter {

    private static final int MAX_ROWS = 400;

    private final FiberBuilder logFiber;
    private final UILogger uiLogger;

    private final WebSocketViews<IMsgBlotterView> views;

    private final Map<String, Boolean> connectedNibblers;

    private final NavigableSet<MsgBlotterRow> rows;
    private final SimpleDateFormat sdf;

    private int rowCount;

    public MsgBlotterPresenter(final FiberBuilder logFiber, final UILogger uiLogger) {

        this.logFiber = logFiber;
        this.uiLogger = uiLogger;

        this.views = WebSocketViews.create(IMsgBlotterView.class, this);

        this.connectedNibblers = new HashMap<>();

        this.rows = new TreeSet<>();
        this.sdf = DateTimeUtil.getDateFormatter(DateTimeUtil.TIME_FORMAT);

        this.rowCount = 0;
    }

    public void setNibblerConnected(final String source, final boolean isConnected) {

        connectedNibblers.put(source, isConnected);
        views.all().setNibblerConnected(source, isConnected);
    }

    public void addLine(final String source, final BlotterLine line) {

        final String time = sdf.format(line.nanoSinceMidnightUTC / DateTimeUtil.NANOS_IN_MILLIS);
        final MsgBlotterRow row = new MsgBlotterRow(++rowCount, line.nanoSinceMidnightUTC, time, source, line.text);
        rows.add(row);

        if (MAX_ROWS < rows.size()) {

            final MsgBlotterRow oldestRow = rows.first();
            rows.remove(oldestRow);

            if (!oldestRow.equals(row)) {

                views.all().removeRow(oldestRow.id);
                views.all().addRow(row.id, row.timestamp, row.source, row.text);
            }
        } else {
            views.all().addRow(row.id, row.timestamp, row.source, row.text);
        }
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

        final IMsgBlotterView newView = views.get(channel);

        for (final Map.Entry<String, Boolean> nibblerConnected : connectedNibblers.entrySet()) {
            newView.setNibblerConnected(nibblerConnected.getKey(), nibblerConnected.getValue());
        }

        for (final MsgBlotterRow row : rows) {
            newView.addRow(row.id, row.timestamp, row.source, row.text);
        }
    }
}
