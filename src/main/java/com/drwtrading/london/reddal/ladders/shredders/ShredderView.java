package com.drwtrading.london.reddal.ladders.shredders;

import com.drwtrading.london.reddal.data.SymbolStackData;
import com.drwtrading.london.reddal.data.ibook.MDForSymbol;
import com.drwtrading.london.reddal.fastui.UiEventHandler;
import com.drwtrading.london.reddal.fastui.UiPipeImpl;
import com.drwtrading.london.reddal.fastui.html.HTML;
import com.drwtrading.london.reddal.workingOrders.WorkingOrdersByID;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class ShredderView implements UiEventHandler {

    static final int INITAL_ORDERS_PER_ROW = 100;
    private static final int NUM_PLUS = 107;
    private static final int NUM_MINUS = 109;
    private static final int PLUS = 187;
    private static final int MINUS = 189;

    private final IShredderUI view;
    private final UiPipeImpl uiPipe;

    private final AtomicLong heartbeatSeqNo;
    private final StringBuilder cmdSB;

    private ShredderBookView shredderBookView;
    private int levels;
    public String symbol;
    private Long lastHeartbeatSentMillis = null;

    ShredderView(final IShredderUI view, final UiPipeImpl uiPipe) {

        this.view = view;
        this.uiPipe = uiPipe;

        this.uiPipe.setHandler(this);

        this.heartbeatSeqNo = new AtomicLong(0L);
        this.cmdSB = new StringBuilder();
    }

    private void setup() {
        view.draw(levels, INITAL_ORDERS_PER_ROW);
        uiPipe.clear();
        uiPipe.scrollable('#' + HTML.LADDER);
    }

    void subscribeToSymbol(final String symbol, final int levels, final MDForSymbol marketData, final WorkingOrdersByID workingOrders,
            final SymbolStackData stackData) {

        this.symbol = symbol;
        this.levels = levels;

        this.shredderBookView = new ShredderBookView(uiPipe, view, marketData, symbol, levels, workingOrders, stackData);

        setup();
    }

    void onRawInboundData(final String data) {
        uiPipe.onInbound(data);
    }

    void refreshAndFlush() {
        shredderBookView.refresh();
        uiPipe.flush();
    }

    @Override
    public void onClick(final String id, final Map<String, String> data) {
        final String button = data.get("button");
        if ("middle".equals(button)) {
            shredderBookView.center();
        }

    }

    @Override
    public void onDblClick(final String id, final Map<String, String> data) {

    }

    @Override
    public void onUpdate(final String id, final Map<String, String> data) {
        if (data.size() < 1) {
            shredderBookView.shreddedRowWidth = Integer.valueOf(id);
        }
    }

    @Override
    public void onScroll(final String direction) {
        resetLastCenteredTime();

        if ("up".equals(direction)) {
            shredderBookView.scrollUp();
        } else if ("down".equals(direction)) {
            shredderBookView.scrollDown();
        }

    }

    private void resetLastCenteredTime() {
    }

    @Override
    public void onKeyDown(final int keyCode) {
        switch (keyCode) {
            case PLUS:
            case NUM_PLUS:
                shredderBookView.zoomIn();
                break;
            case MINUS:
            case NUM_MINUS:
                shredderBookView.zoomOut();
        }
    }

    @Override
    public void onHeartbeat(final long sentTimeMillis) {
        if (lastHeartbeatSentMillis == sentTimeMillis) {
            lastHeartbeatSentMillis = null;
        }
    }

    void sendHeartbeat() {
        lastHeartbeatSentMillis = System.currentTimeMillis();
        uiPipe.send(UiPipeImpl.cmd(cmdSB, "heartbeat", lastHeartbeatSentMillis, heartbeatSeqNo.getAndIncrement()));
    }

    void timedRefresh() {
        shredderBookView.refresh();
        uiPipe.flush();
    }
}