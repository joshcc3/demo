package com.drwtrading.london.reddal.picard;

import com.drwtrading.london.eeif.utils.formatting.NumberFormatUtil;
import com.drwtrading.london.eeif.utils.staticData.InstType;
import com.drwtrading.london.reddal.ladders.RecenterLadder;
import com.drwtrading.london.reddal.stacks.StackRunnableInfo;
import com.drwtrading.london.reddal.symbols.DisplaySymbol;
import com.drwtrading.london.reddal.util.UILogger;
import com.drwtrading.london.websocket.FromWebSocketView;
import com.drwtrading.london.websocket.WebSocketViews;
import com.drwtrading.websockets.WebSocketClient;
import com.drwtrading.websockets.WebSocketConnected;
import com.drwtrading.websockets.WebSocketControlMessage;
import com.drwtrading.websockets.WebSocketDisconnected;
import com.drwtrading.websockets.WebSocketInboundData;
import org.jetlang.channels.Publisher;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class PicardUI {

    private static final long FLUSH_PERIOD_MILLIS = 300;

    private final UILogger webLog;

    private final Set<InstType> instType;

    private final PicardSounds sounds;

    private final Publisher<RecenterLadder> reddalMessagePublisher;

    private final DecimalFormat bpsDF;

    private final Map<String, String> displaySymbols;
    private final Map<String, PicardRow> cache;
    private final Map<String, PicardRow> dirty;

    private final Set<String> opxlFilterSymbols;
    private final Set<String> nonRunnableSymbols;

    private final WebSocketViews<IPicardView> views;

    public PicardUI(final UILogger webLog, final Set<InstType> instTypes, final PicardSounds sounds,
            final Publisher<RecenterLadder> recenterLadderPublisher) {

        this.webLog = webLog;

        this.instType = instTypes;

        this.sounds = sounds;

        this.reddalMessagePublisher = recenterLadderPublisher;

        this.bpsDF = NumberFormatUtil.getDF(NumberFormatUtil.THOUSANDS, 2);

        this.displaySymbols = new HashMap<>();
        this.cache = new HashMap<>();
        this.dirty = new HashMap<>();

        this.opxlFilterSymbols = new HashSet<>();
        this.nonRunnableSymbols = new HashSet<>();

        this.views = new WebSocketViews<>(IPicardView.class, this);
    }

    public void addPicardRow(final PicardRow row) {

        if (instType.contains(row.instType)) {
            dirty.put(row.symbol, row);
        }
    }

    public void setDisplaySymbol(final DisplaySymbol displaySymbol) {
        displaySymbols.put(displaySymbol.marketDataSymbol, displaySymbol.marketDataSymbol);
    }

    public void setOPXLFilterList(final Set<String> symbols) {

        opxlFilterSymbols.clear();
        opxlFilterSymbols.addAll(symbols);
    }

    public void setSymbolRunnable(final StackRunnableInfo info) {
        if (info.isRunnable) {
            nonRunnableSymbols.remove(info.symbol);
        } else {
            nonRunnableSymbols.add(info.symbol);
        }
    }

    public void webControl(final WebSocketControlMessage msg) {

        if (msg instanceof WebSocketConnected) {

            webUIConnected((WebSocketConnected) msg);

        } else if (msg instanceof WebSocketDisconnected) {

            views.unregister((WebSocketDisconnected) msg);

        } else if (msg instanceof WebSocketInboundData) {

            inboundData((WebSocketInboundData) msg);
        }
    }

    private void webUIConnected(final WebSocketConnected connected) {

        final IPicardView view = views.register(connected);
        view.setSound(sounds.fileName);
        for (final PicardRow opportunity : cache.values()) {
            display(view, opportunity);
        }
    }

    private void inboundData(final WebSocketInboundData msg) {

        webLog.write("LadderWorkspace", msg);
        views.invoke(msg);
    }

    @FromWebSocketView
    public void recenter(final WebSocketClient webSocketClient, final String symbol, final String value) {

        final RecenterLadder recenterLadder = new RecenterLadder(webSocketClient.getUserName(), symbol, Long.parseLong(value));
        reddalMessagePublisher.publish(recenterLadder);
    }

    public long flush() {

        for (final PicardRow picardRow : dirty.values()) {
            display(views.all(), picardRow);
        }
        cache.putAll(dirty);
        dirty.clear();

        return FLUSH_PERIOD_MILLIS;
    }

    private void display(final IPicardView view, final PicardRow row) {

        final String bpsThrough = bpsDF.format(row.bpsThrough);
        final String givenSymbol = displaySymbols.get(row.symbol);
        final String opportunitySize = bpsDF.format(row.opportunitySize);
        final String ccy = row.ccy != null ? row.ccy.name() : "";

        final String displaySymbol;
        if (null == givenSymbol) {
            displaySymbol = row.symbol;
        } else {
            displaySymbol = givenSymbol;
        }

        final boolean isOnOPXLFilterList = opxlFilterSymbols.contains(row.symbol);
        final boolean isRunnable = !nonRunnableSymbols.contains(row.symbol);

        view.picard(row.symbol, displaySymbol, row.side.toString(), bpsThrough, opportunitySize, ccy, row.prettyPrice, row.description,
                row.state.toString(), row.inAuction, row.isNewRow, isOnOPXLFilterList, isRunnable);
    }
}
